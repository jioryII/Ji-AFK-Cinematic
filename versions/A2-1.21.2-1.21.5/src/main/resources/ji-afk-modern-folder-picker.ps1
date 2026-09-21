$ErrorActionPreference = 'Stop'
Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

[Flags]
public enum FOS : uint {
    PICKFOLDERS = 0x20,
    FORCEFILESYSTEM = 0x40,
    PATHMUSTEXIST = 0x800
}

public enum SIGDN : uint { FILESYSPATH = 0x80058000 }

[ComImport, Guid("43826D1E-E718-42EE-BC55-A1E261C37BFE"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
public interface IShellItem {
    void BindToHandler(IntPtr pbc, [MarshalAs(UnmanagedType.LPStruct)] Guid bhid,
        [MarshalAs(UnmanagedType.LPStruct)] Guid riid, out IntPtr ppv);
    void GetParent(out IShellItem ppsi);
    void GetDisplayName(SIGDN sigdnName, out IntPtr ppszName);
    void GetAttributes(uint sfgaoMask, out uint psfgaoAttribs);
    void Compare(IShellItem psi, uint hint, out int piOrder);
}

[ComImport, Guid("42F85136-DB7E-439C-85F1-E4075D135FC8"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
public interface IFileDialog {
    [PreserveSig] int Show(IntPtr parent);
    void SetFileTypes(uint count, IntPtr filters);
    void SetFileTypeIndex(uint index);
    void GetFileTypeIndex(out uint index);
    void Advise(IntPtr events, out uint cookie);
    void Unadvise(uint cookie);
    void SetOptions(FOS options);
    void GetOptions(out FOS options);
    void SetDefaultFolder(IShellItem item);
    void SetFolder(IShellItem item);
    void GetFolder(out IShellItem item);
    void GetCurrentSelection(out IShellItem item);
    void SetFileName([MarshalAs(UnmanagedType.LPWStr)] string name);
    void GetFileName([MarshalAs(UnmanagedType.LPWStr)] out string name);
    void SetTitle([MarshalAs(UnmanagedType.LPWStr)] string title);
    void SetOkButtonLabel([MarshalAs(UnmanagedType.LPWStr)] string text);
    void SetFileNameLabel([MarshalAs(UnmanagedType.LPWStr)] string label);
    void GetResult(out IShellItem item);
    void AddPlace(IShellItem item, uint alignment);
    void SetDefaultExtension([MarshalAs(UnmanagedType.LPWStr)] string extension);
    void Close(int hr);
    void SetClientGuid(ref Guid guid);
    void ClearClientData();
    void SetFilter(IntPtr filter);
}

[ComImport, Guid("DC1C5A9C-E88A-4DDE-A5A1-60F82A20AEF7")]
public class FileOpenDialog { }

public static class ModernFolderPicker {
    [DllImport("shell32.dll", CharSet = CharSet.Unicode, PreserveSig = false)]
    private static extern void SHCreateItemFromParsingName(string path, IntPtr bindContext,
        ref Guid riid, [MarshalAs(UnmanagedType.Interface)] out IShellItem item);

    public static string Pick(string initialPath, string title) {
        IFileDialog dialog = (IFileDialog)new FileOpenDialog();
        try {
            dialog.SetOptions(FOS.PICKFOLDERS | FOS.FORCEFILESYSTEM | FOS.PATHMUSTEXIST);
            dialog.SetTitle(title);
            if (!String.IsNullOrWhiteSpace(initialPath)) {
                Guid shellItemId = typeof(IShellItem).GUID;
                IShellItem initial;
                SHCreateItemFromParsingName(initialPath, IntPtr.Zero, ref shellItemId, out initial);
                dialog.SetFolder(initial);
                Marshal.FinalReleaseComObject(initial);
            }
            int result = dialog.Show(IntPtr.Zero);
            if (result != 0) return null;
            IShellItem selected;
            dialog.GetResult(out selected);
            try {
                IntPtr value;
                selected.GetDisplayName(SIGDN.FILESYSPATH, out value);
                try { return Marshal.PtrToStringUni(value); }
                finally { Marshal.FreeCoTaskMem(value); }
            } finally { Marshal.FinalReleaseComObject(selected); }
        } finally { Marshal.FinalReleaseComObject(dialog); }
    }
}
'@
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$selected = [ModernFolderPicker]::Pick(
    $env:JI_AFK_INITIAL_MUSIC_FOLDER,
    $env:JI_AFK_FOLDER_PICKER_TITLE)
if ($null -ne $selected) { [Console]::Out.Write($selected) }
