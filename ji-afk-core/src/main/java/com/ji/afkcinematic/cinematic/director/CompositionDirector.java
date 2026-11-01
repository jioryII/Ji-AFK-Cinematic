package com.ji.afkcinematic.cinematic.director;

import com.ji.afkcinematic.cinematic.CameraFrame;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.SplittableRandom;

/** Bounded scene-aware selection. Safety filters precede variety and weighting. */
public final class CompositionDirector {
    public static final int MAX_SCENE_QUERIES = 384;
    public static final int PATH_SAMPLES = 4;
    public static final double CAMERA_RADIUS = .12;
    private static final double[] LENS_YAW = {0, -1, 1, 0, 0, -1, -1, 1, 1};
    private static final double[] LENS_PITCH = {0, 0, 0, -1, 1, -1, 1, -1, 1};

    /** Distance to the first solid surface; a miss returns the requested distance. */
    @FunctionalInterface public interface Scene {
        double clearDistance(CameraVector origin, CameraVector direction, double distance);
    }
    public record Selection(CompositionPath path, CameraFrame fallbackFrame, double score,
                            int sceneQueries, String reason) {
        public boolean fallback() { return path == null; }
    }
    public record Assessment(boolean safe, double score, CameraVector start, String reason, ViewSignature view) {}
    private record Memory(String id, ViewSignature view) {}
    private ViewSignature previousExit;
    public void observeExit(CameraFrame frame, CompositionPath.Subject subject) {
        if (frame != null && Double.isFinite(frame.x()) && Double.isFinite(frame.y()) && Double.isFinite(frame.z()))
            previousExit = ViewSignature.of(frame, subject, 0);
    }
    private static final CameraVector[] AXES = {new CameraVector(1, 0, 0), new CameraVector(0, 1, 0), new CameraVector(0, 0, 1)};
    private static final CameraVector[] VOLUME_OFFSETS = volumeOffsets();
    private static CameraVector[] volumeOffsets() {
        CameraVector[] offsets = new CameraVector[9]; offsets[0] = CameraVector.ZERO;
        for (int i = 0; i < 8; i++) offsets[i + 1] = new CameraVector(
                (i & 1) == 0 ? -CAMERA_RADIUS : CAMERA_RADIUS,
                (i & 2) == 0 ? -CAMERA_RADIUS : CAMERA_RADIUS,
                (i & 4) == 0 ? -CAMERA_RADIUS : CAMERA_RADIUS);
        return offsets;
    }
    private record Option(CompositionPath path, Assessment assessment) {}
    private final Deque<Memory> history = new ArrayDeque<>();

    public void reset() { history.clear(); previousExit = null; }

    public Selection selectHorizon(CompositionPreset preset, CompositionPath.Subject subject, double heading,
                                   double duration, double speed, long seed, Scene world) {
        Budget scene = new Budget(world, MAX_SCENE_QUERIES);
        SplittableRandom random = new SplittableRandom(seed);
        List<Option> candidates = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            CompositionPath base = variant(preset, heading, i, random);
            CompositionPath path = new CompositionPath(preset, base.variation(), heading, heading);
            Assessment assessment = assess(path, subject, duration, speed, scene);
            if (assessment.safe) candidates.add(new Option(path, assessment));
        }
        if (candidates.isEmpty()) return select(preset, subject, heading, duration, speed, seed, scene);
        boolean hasDistinct = candidates.stream().anyMatch(option -> immediateDifference(option.assessment.view) >= .65);
        Option best = candidates.stream().filter(option -> !hasDistinct || immediateDifference(option.assessment.view) >= .65)
                .max(java.util.Comparator.comparingDouble(option -> option.assessment.score)).orElseThrow();
        history.addFirst(new Memory(preset.id(), best.assessment.view));
        while (history.size() > 16) history.removeLast();
        return new Selection(best.path, null, best.assessment.score, scene.used, "TIME_HORIZON");
    }

    public Selection select(CompositionPreset requested, CompositionPath.Subject subject,
                            double yaw, double duration, double speed, long seed, Scene world) {
        Budget scene = world instanceof Budget budget ? budget : new Budget(world, MAX_SCENE_QUERIES);
        SplittableRandom random = new SplittableRandom(seed);
        List<Option> safe = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            CompositionPath path = variant(requested, yaw, i, random);
            Assessment result = assess(path, subject, duration, speed, scene);
            if (result.safe) safe.add(new Option(path, result));
        }
        boolean adapted = safe.isEmpty();
        if (adapted) {
            // Probe actual nearby directions before adapting a wide move. Depth is capped
            // at what a close take needs; a large exterior opening cannot monopolize it.
            List<Integer> headings = new ArrayList<>();
            double[] depths = new double[16];
            double bestDepth = 0;
            for (int i = 0; i < depths.length; i++) {
                depths[i] = scene.trace(subject.eye(), CameraVector.direction(yaw + i * 22.5, 0), 3);
                bestDepth = Math.max(bestDepth, depths[i]);
            }
            for (int i = 0; i < depths.length; i++) if (depths[i] >= Math.min(2.5, bestDepth) - .2) headings.add(i);
            // Seeded shuffle, preserving reproducibility without global random state.
            for (int i = headings.size() - 1; i > 0; i--) {
                int other = random.nextInt(i + 1);
                int value = headings.get(i); headings.set(i, headings.get(other)); headings.set(other, value);
            }
            for (int i = 0; i < 2; i++) {
                int heading = headings.isEmpty() ? i * 8 : headings.get(i % headings.size());
                double relativeYaw = heading * 22.5;
                if (relativeYaw > 180) relativeYaw -= 360;
                boolean frontal = requested.character && Math.abs(relativeYaw) <= 50;
                // Adapt within the configured pool: an environment-only sequence must
                // not silently turn into portraits when a large landscape move cannot fit.
                CompositionPreset preset = requested.character
                        ? (frontal ? CompositionPreset.TIGHT_PORTRAIT : CompositionPreset.EYE_LEVEL_ARC)
                        : (i == 0 ? CompositionPreset.HORIZON_ARC : CompositionPreset.PARALLAX_TRUCK);
                double relativeAngle = frontal ? relativeYaw : 90 + relativeYaw - preset.angleOffset;
                double scale = requested.character ? (frontal ? .88 : .43) : .06;
                double height = requested.character ? 1 : subject.eyeHeight() / preset.startHeight;
                CompositionPath path = new CompositionPath(preset, new CompositionPath.Variation(relativeAngle,
                        scale, height, random.nextBoolean() ? 1 : -1, 0, .12), yaw);
                Assessment result = assess(path, subject, duration, speed, scene);
                if (result.safe) safe.add(new Option(path, result));
            }
        }
        if (safe.isEmpty()) {
            // No unsafe candidate is promoted for variety. Stay at the player's safe anchor
            // and choose an actual open viewing direction with the remaining query budget.
            double best = -1, bestYaw = yaw;
            for (int i = 0; i < 8; i++) {
                double candidateYaw = yaw + i * 45;
                double depth = scene.trace(subject.eye(), CameraVector.direction(candidateYaw, 0), 12);
                if (depth > best) { best = depth; bestYaw = candidateYaw; }
            }
            CameraVector eye = subject.eye();
            return new Selection(null, new CameraFrame(eye.x(), eye.y(), eye.z(), 0, (float) bestYaw, 0),
                    0, scene.used, "NO_SAFE_COMPOSITION");
        }
        // Safety has already passed. Require a visible cut whenever a viable alternative exists.
        List<Option> distinct = safe.stream().filter(option -> immediateDifference(
                option.assessment.view) >= .65).toList();
        if (!distinct.isEmpty()) safe = new ArrayList<>(distinct);
        double best = safe.stream().mapToDouble(option -> option.assessment.score).max().orElse(0);
        List<Option> shortlist = safe.stream().filter(option -> option.assessment.score >= best - .18).toList();
        double total = shortlist.stream().mapToDouble(option -> weight(option.assessment.score, best)).sum();
        double pick = random.nextDouble() * total;
        Option selected = shortlist.getLast();
        for (Option option : shortlist) {
            pick -= weight(option.assessment.score, best);
            if (pick <= 0) { selected = option; break; }
        }
        history.addFirst(new Memory(selected.path.preset().id(), selected.assessment.view));
        while (history.size() > 16) history.removeLast();
        return new Selection(selected.path, null, selected.assessment.score, scene.used,
                adapted ? "ADAPTED_TO_AVAILABLE_SPACE" : "SAFE_WEIGHTED_VARIANT");
    }

    private static double weight(double score, double best) { return Math.exp((score - best) * 5); }

    private CompositionPath variant(CompositionPreset preset, double yaw, int index, SplittableRandom random) {
        double angle = index * 90 + random.nextDouble(-45, 45);
        if (preset.motion == CompositionPreset.Motion.PORTRAIT) angle = -45 + index * 30 + random.nextDouble(-7, 7);
        double scale = random.nextDouble(.82, 1.08);
        if (preset == CompositionPreset.TIGHT_PORTRAIT) scale = random.nextDouble(.72, 1.0);
        return new CompositionPath(preset, new CompositionPath.Variation(angle, scale,
                random.nextDouble(.9, 1.1), random.nextBoolean() ? 1 : -1,
                random.nextBoolean() ? 1 : -1), yaw);
    }

    /** Four samples include both endpoints; every native frame also resolves its swept camera. */
    public Assessment assess(CompositionPath path, CompositionPath.Subject subject,
                              double duration, double speed, Scene world) {
        Budget scene = world instanceof Budget budget ? budget : new Budget(world, MAX_SCENE_QUERIES);
        double minimumPath = 1, minimumDepth = 1, meanDepth = 0;
        CameraVector first = null, previous = null;
        for (int i = 0; i < PATH_SAMPLES; i++) {
            CompositionPath.Pose pose = path.sample(subject, Math.max(0, duration) * i / (PATH_SAMPLES - 1), speed);
            if (!pose.position().finite() || !pose.target().finite()) return rejected("NON_FINITE_PATH");
            // Physical trajectory and volume, independent of the subject tether.
            CameraVector resolved = previous == null ? pose.position() : resolveCollision(previous, pose.position(), scene);
            for (CameraVector direction : AXES) {
                if (scene.trace(pose.position().subtract(direction.scale(CAMERA_RADIUS)), direction, CAMERA_RADIUS * 2)
                        < CAMERA_RADIUS * 2 - .01) return rejected("CAMERA_VOLUME_BLOCKED");
            }
            if (first == null) first = resolved;
            double segment = previous == null ? 0 : previous.distance(pose.position());
            double retained = previous == null || segment < .01 ? 1 : previous.distance(resolved) / segment;
            previous = resolved;
            if (retained < .72 || scene.exhausted) return rejected("PATH_OBSTRUCTED");
            minimumPath = Math.min(minimumPath, Math.min(1, retained));
            CameraFrame frame = pose.at(resolved);
            if (path.preset().character) {
                Projection projection = new Projection(frame, subject);
                CameraVector subjectPoint = subject.position().add(new CameraVector(0, subject.eyeHeight() * .9, 0));
                CameraVector delta = subjectPoint.subtract(resolved);
                if (scene.trace(resolved, delta.normalized(), delta.length()) + .06 < delta.length()
                        || !projection.contains(subjectPoint.x(), subjectPoint.y(), subjectPoint.z(), .88)) return rejected("SUBJECT_OCCLUDED_OR_OUT_OF_FRAME");
                if (!headFitsFrame(projection, subject)) return rejected("HEAD_CROPPED_BY_FRAME");
            }
            int blocked = 0;
            double sampleDepth = 0;
            double required = path.preset().requiredDepth();
            for (int ray = 0; ray < LENS_YAW.length; ray++) {
                CameraVector direction = CameraVector.direction(frame.yaw() + LENS_YAW[ray] * subject.horizontalFov() * .32,
                        frame.pitch() + LENS_PITCH[ray] * subject.verticalFov() * .32);
                double depth = scene.trace(resolved, direction, required);
                if (depth < 1.25) blocked++;
                if (ray == 0 && !path.preset().character && depth < 1.5) return rejected("LENS_FACES_NEAR_WALL");
                if (ray == 0 && path.preset().motion == CompositionPreset.Motion.SHOULDER && depth < 4)
                    return rejected("SHOULDER_FACES_NEAR_WALL");
                // Saturates at the space this composition needs, not at maximum openness.
                sampleDepth += Math.min(1, depth / required) / LENS_YAW.length;
            }
            if (blocked >= 5 || scene.exhausted) return rejected("LENS_DOMINATED_BY_NEAR_SURFACES");
            minimumDepth = Math.min(minimumDepth, sampleDepth);
            meanDepth += sampleDepth / PATH_SAMPLES;
        }
        ViewSignature view = ViewSignature.start(path, subject, duration, speed);
        double novelty = novelty(view);
        double score = .3 * minimumPath + .3 * minimumDepth + .2 * meanDepth + .2 * novelty;
        return new Assessment(true, score, first, "SAFE", view);
    }

    private double immediateDifference(ViewSignature view) {
        if (previousExit != null) return view.difference(previousExit);
        return history.isEmpty() ? 1 : view.difference(history.getFirst().view);
    }
    private double novelty(ViewSignature view) {
        double penalty = 1 - immediateDifference(view), recency = .8;
        for (Memory previous : history) {
            penalty = Math.max(penalty, recency * (1 - view.difference(previous.view)));
            recency *= .84;
        }
        return 1 - penalty;
    }

    private static Assessment rejected(String reason) { return new Assessment(false, 0, CameraVector.ZERO, reason, null); }

    public static boolean subjectInFrame(CameraFrame frame, CameraVector point, CompositionPath.Subject subject) {
        return pointInFrame(frame, point, subject, .88);
    }

    /** Conservative vanilla head bounds, including the useful image between default letterbox bars.
     * This is projected geometry, not semantic face/skin recognition. No additional world queries. */
    public static boolean headFitsFrame(CameraFrame frame, CompositionPath.Subject subject) {
        return headFitsFrame(new Projection(frame, subject), subject);
    }
    private static boolean headFitsFrame(Projection projection, CompositionPath.Subject subject) {
        double radius = Math.min(.25, subject.height() / 3);
        double bottom = Math.max(.05, subject.eyeHeight() - .38);
        double top = Math.min(subject.height(), subject.eyeHeight() + .18);
        for (int corner = 0; corner < 8; corner++) {
            if (!projection.contains(subject.position().x() + ((corner & 1) == 0 ? -radius : radius),
                    subject.position().y() + ((corner & 2) == 0 ? bottom : top),
                    subject.position().z() + ((corner & 4) == 0 ? -radius : radius), .68)) return false;
        }
        return true;
    }
    private static boolean pointInFrame(CameraFrame frame, CameraVector point, CompositionPath.Subject subject, double verticalMargin) {
        return new Projection(frame, subject).contains(point.x(), point.y(), point.z(), verticalMargin);
    }
    /** One camera basis per sample, shared by subject and all eight head corners. */
    private static final class Projection {
        final CameraFrame frame;
        final CameraVector forward, right, up;
        final double tan, aspect, cosRoll, sinRoll;
        Projection(CameraFrame frame, CompositionPath.Subject subject) {
            this.frame = frame;
            forward = CameraVector.direction(frame.yaw(), frame.pitch());
            double yaw = Math.toRadians(frame.yaw()), pitch = Math.toRadians(frame.pitch());
            right = new CameraVector(Math.cos(yaw), 0, Math.sin(yaw));
            up = new CameraVector(-Math.sin(yaw) * Math.sin(pitch), Math.cos(pitch), Math.cos(yaw) * Math.sin(pitch));
            tan = Math.tan(Math.toRadians(subject.verticalFov() / 2)); aspect = subject.aspect();
            double roll = Math.toRadians(frame.roll()); cosRoll = Math.cos(roll); sinRoll = Math.sin(roll);
        }
        boolean contains(double x, double y, double z, double verticalMargin) {
            CameraVector delta = new CameraVector(x - frame.x(), y - frame.y(), z - frame.z());
            double depth = forward.dot(delta);
            if (!(depth > .25)) return false;
            double horizontal = delta.dot(right), vertical = delta.dot(up);
            return Math.abs(horizontal * cosRoll + vertical * sinRoll) < depth * tan * aspect * .88
                    && Math.abs(vertical * cosRoll - horizontal * sinRoll) < depth * tan * verticalMargin;
        }
    }

    /** Centre and eight corners of a small camera volume swept from its safe anchor. */
    public static CameraVector resolveCollision(CameraVector anchor, CameraVector desired, Scene scene) {
        CameraVector delta = desired.subtract(anchor);
        double distance = delta.length();
        if (!(distance > 1.0e-6) || !Double.isFinite(distance)) return anchor;
        CameraVector direction = delta.scale(1 / distance);
        double clear = distance;
        for (CameraVector offset : VOLUME_OFFSETS) {
            double sample = scene.clearDistance(anchor.add(offset), direction, distance);
            sample = Double.isFinite(sample) ? Math.max(0, Math.min(distance, sample)) : 0;
            clear = Math.min(clear, sample < distance ? Math.max(0, sample - .18) : distance);
        }
        return anchor.add(direction.scale(clear));
    }

    private static final class Budget implements Scene {
        final Scene delegate;
        final int limit;
        int used;
        boolean exhausted;
        Budget(Scene delegate, int limit) { this.delegate = delegate; this.limit = limit; }
        @Override public double clearDistance(CameraVector origin, CameraVector direction, double distance) {
            return trace(origin, direction, distance);
        }
        double trace(CameraVector origin, CameraVector direction, double distance) {
            if (used >= limit) { exhausted = true; return 0; }
            if (!origin.finite() || !direction.finite() || !Double.isFinite(distance) || distance < 0) return 0;
            used++;
            double result = delegate.clearDistance(origin, direction, distance);
            double clear = Double.isFinite(result) ? Math.max(0, Math.min(distance, result)) : 0;
            return clear;
        }
    }
}
