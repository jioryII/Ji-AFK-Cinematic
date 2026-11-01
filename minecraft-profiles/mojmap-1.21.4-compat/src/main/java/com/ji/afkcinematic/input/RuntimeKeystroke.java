package com.ji.afkcinematic.input;
/** Data from the primitive GLFW callback; no synthetic Minecraft event. */
public record RuntimeKeystroke(int key, int scancode, int modifiers) { }
