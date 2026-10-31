package com.gem.keeppeer;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Framework-only external UI harness for the actual installed minified APK.
 * No substituted Application, app-symbol linkage, reflection keeps or shortcuts.
 * Each invocation performs one explicitly selected journey on a verified NEW
 * Phase5 fixture and reports exact completion counts. This is not JUnit.
 */
public final class Phase5StandaloneRunner extends Instrumentation {
    private Bundle arguments;
    private int originalServiceFlags = -1;
    private String stage = "startup";
    private static final String MASTER = "Phase5 synthetic master 2026";
    private static final String CHANGED = "Phase5 changed synthetic master";
    private static final String TITLE = "Phase5 synthetic entry";
    private static final String WRONG = "Could not unlock. Check your password. Damaged or missing device keys or vault files also prevent access. Existing data was preserved.";

    @Override public void onCreate(Bundle values) {
        arguments = values;
        super.onCreate(values);
        start();
    }
    @Override public void onStart() {
        long started = SystemClock.elapsedRealtime();
        Bundle result = new Bundle();
        try {
            android.accessibilityservice.AccessibilityServiceInfo serviceInfo=getUiAutomation().getServiceInfo();
            originalServiceFlags=serviceInfo.flags;
            serviceInfo.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            getUiAutomation().setServiceInfo(serviceInfo);
            guard();
            checkpoint("started");
            String selection = arguments.getString("phase5Journey", "");
            switch (selection) {
                case "ordinaryCompatibilitySetupAndReopen": compatibility(); break;
                case "ordinaryCompatibilityReopen": launch(); unlock(MASTER); await(TITLE); settings(); action("Lock now"); unlock(MASTER); await(TITLE); checkpoint("minified-normal-cold-and-manual-lock-reopen"); break;
                case "ordinaryApplicationMinifiedJourney": whole(); break;
                case "ordinaryApplicationColdReopen": coldReopen(); break;
                case "seedOnlyDedicatedLegacyFixture": seedLegacy(); break;
                case "ordinaryMinifiedLegacyRefusalPreservesFixture": refuseLegacy(); break;
                default: throw new AssertionError("Unknown explicit journey");
            }
            result.putString("stream", "\nPhase5Harness: PASS expected=1 started=1 completed=1 passed=1 failed=0 errors=0 skipped=0\nDurationMs=" + (SystemClock.elapsedRealtime()-started) + "\n");
            finish(-1, result);
        } catch (Throwable failure) {
            result.putString("stream", "\nPhase5Harness: FAIL expected=1 started=1 completed=1 passed=0 failed=1 skipped=0\nStage=" + stage + "\nFailure=" + failure.getClass().getSimpleName() + "\nReason=" + failure.getMessage() + "\nDurationMs=" + (SystemClock.elapsedRealtime()-started) + "\n");
            finish(-2, result);
        }
    }
    @Override public void finish(int code, Bundle result) {
        if (originalServiceFlags>=0) {
            try {
                android.accessibilityservice.AccessibilityServiceInfo info=getUiAutomation().getServiceInfo();
                info.flags=originalServiceFlags; getUiAutomation().setServiceInfo(info);
                originalServiceFlags=-1;
            } catch (RuntimeException failure) {
                code=-2;
                result.putString("stream","Phase5Harness: FAIL UIAutomation flag restoration failed; no pass claimed");
            }
        }
        super.finish(code,result);
    }
    private static void require(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
    private String shell(String command) throws IOException {
        try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand(command));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] data = new byte[4096]; int count;
            while ((count=input.read(data))!=-1) out.write(data,0,count);
            return out.toString("UTF-8").trim();
        }
    }
    private String apkHash(String path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new FileInputStream(path)) {
            byte[] data = new byte[65536]; int count;
            while ((count=input.read(data))!=-1) digest.update(data,0,count);
        }
        StringBuilder out = new StringBuilder();
        for (byte value:digest.digest()) out.append(String.format(Locale.ROOT,"%02x",value & 255));
        return out.toString();
    }
    private void guard() throws Exception {
        stage="identity";
        String expected=arguments.getString("phase5Avd","");
        require(expected.startsWith("Keeppeer_Guard_Phase5_"),"Dedicated fixture required");
        String actual = shell("getprop ro.boot.qemu.avd_name");
        if (actual.isEmpty()) actual = shell("getprop ro.kernel.qemu.avd_name");
        if (actual.isEmpty()) actual = shell("getprop qemu.avd_name");
        require(expected.equals(actual),"Exact AVD mismatch");
        require("1".equals(shell("getprop ro.kernel.qemu")),"Not qemu");
        require("1".equals(shell("getprop sys.boot_completed")),"Boot incomplete");
        ApplicationInfo info=getTargetContext().getApplicationInfo();
        require("com.gem.keeppeer".equals(info.packageName),"Wrong package");
        require((info.flags & ApplicationInfo.FLAG_DEBUGGABLE)==0,"Target must be non-debuggable");
        require("com.gem.keeppeer.KeeppeerApplication".equals(getTargetContext().getApplicationContext().getClass().getName()),"Ordinary Application required");
        String app=apkHash(getTargetContext().getPackageCodePath());
        String test=apkHash(getContext().getPackageCodePath());
        require(app.equals(arguments.getString("phase5ExpectedAppSha256")),"Host/installed app hash mismatch");
        require(test.equals(arguments.getString("phase5ExpectedTestSha256")),"Host/installed test hash mismatch");
        Bundle values=new Bundle();
        values.putString("phase5InstalledAppSha256",app);
        values.putString("phase5InstalledTestSha256",test);
        values.putLong("phase5PageSize",android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE));
        sendStatus(2,values);
    }
    private List<AccessibilityNodeInfo> nodes() {
        List<AccessibilityNodeInfo> list=new ArrayList<>();
        AccessibilityNodeInfo root=null;
        for (android.view.accessibility.AccessibilityWindowInfo window:getUiAutomation().getWindows()) {
            if (window.getRoot()!=null && "com.gem.keeppeer".equals(String.valueOf(window.getRoot().getPackageName()))) {
                root=window.getRoot(); break;
            }
        }
        if (root==null) root=getUiAutomation().getRootInActiveWindow();
        if(root!=null) visit(root,list);
        return list;
    }
    private void visit(AccessibilityNodeInfo node,List<AccessibilityNodeInfo> list) {
        list.add(node);
        for(int i=0;i<node.getChildCount();i++) {
            AccessibilityNodeInfo child=node.getChild(i);
            if(child!=null) visit(child,list);
        }
    }
    private AccessibilityNodeInfo matching(String label) {
        for(AccessibilityNodeInfo node:nodes()) {
            boolean matches = label.equalsIgnoreCase(String.valueOf(node.getText())) ||
                label.equals(String.valueOf(node.getContentDescription())) ||
                (android.os.Build.VERSION.SDK_INT>=26 && label.equals(String.valueOf(node.getHintText())));
            if (matches) {
                if (node.isVisibleToUser()) return node;
                AccessibilityNodeInfo parent = node.getParent();
                if (parent != null && parent.isVisibleToUser() && (parent.isClickable() || parent.isFocusable())) return node;
            }
        }
        return null;
    }
    private AccessibilityNodeInfo await(String label) { return await(label,30000); }
    private AccessibilityNodeInfo await(String label,long timeout) {
        stage="await "+label;
        long end=SystemClock.elapsedRealtime()+timeout;
        long traceStarted=SystemClock.elapsedRealtime(), nextTrace=traceStarted+10000;
        while(SystemClock.elapsedRealtime()<end) {
            AccessibilityNodeInfo node=matching(label);
            if(node!=null) return node;
            long traceNow=SystemClock.elapsedRealtime();
            if(traceNow>=nextTrace) {
                Bundle tick=new Bundle();
                tick.putString("phase5AwaitTick",label+" elapsed="+(traceNow-traceStarted));
                sendStatus(2,tick);
                nextTrace=traceNow+10000;
            }
            SystemClock.sleep(150);
        }
        Bundle failureState=new Bundle();
        failureState.putString("phase5AwaitFailureControls",controlSummary());
        sendStatus(2,failureState);
        throw new AssertionError("Expected UI control unavailable: "+label+"; "+controlSummary());
    }
    private void click(String label) {
        AccessibilityNodeInfo node=await(label);
        while(node!=null && !node.isClickable()) node=node.getParent();
        require(node!=null && node.performAction(AccessibilityNodeInfo.ACTION_CLICK),"Click failed: "+label);
        SystemClock.sleep(350);
    }
    private void field(String label,String value) {
        scrollTo(label);
        AccessibilityNodeInfo node=await(label);
        while (node!=null && (node.getActions() & AccessibilityNodeInfo.ACTION_SET_TEXT)==0) node=node.getParent();
        require(node!=null,"Expected editable owner for label: "+label);
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        SystemClock.sleep(150);
        Bundle values=new Bundle();
        values.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);
        require(node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,values),"Text replacement failed: "+label);
        node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS);
        SystemClock.sleep(150);
    }
    private void scrollTo(String label) {
        for (int direction : new int[]{AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD}) {
            for(int i=0;i<24;i++) {
                if(matching(label)!=null) return;
                boolean moved=false;
                for(AccessibilityNodeInfo node:nodes()) if(node.isScrollable() && node.performAction(direction)) {
                    moved=true;
                    SystemClock.sleep(350);
                    break;
                }
                if (!moved) {
                    try {
                        if (direction == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
                            shell("input swipe 500 1200 500 500 200");
                        } else {
                            shell("input swipe 500 500 500 1200 200");
                        }
                        SystemClock.sleep(350);
                    } catch (Exception ignored) {}
                }
            }
        }
        await(label);
    }
    private String controlSummary() {
        StringBuilder out = new StringBuilder("Known visible controls=");
        for (String label : new String[]{"Keeppeer","Security","Security settings","Lock now","Appearance","System","Dark","Back","Add","Edit","Keeppeer locked"}) {
            if (matching(label)!=null) out.append(label).append(';');
        }
        Set<String> packages=new HashSet<>();
        for (AccessibilityNodeInfo node:nodes()) packages.add(String.valueOf(node.getPackageName()));
        out.append(" packages=").append(packages).append(" nodes=");
        for (AccessibilityNodeInfo node:nodes()) {
            android.graphics.Rect bounds=new android.graphics.Rect(); node.getBoundsInScreen(bounds);
            out.append(node.getClassName()).append(":visible=").append(node.isVisibleToUser())
                .append(":actions=").append(node.getActions()).append(":bounds=").append(bounds.toShortString()).append('|');
        }
        return out.toString();
    }
    private void action(String label) { scrollTo(label); click(label); }
    private void launch() {
        Intent intent=getTargetContext().getPackageManager().getLaunchIntentForPackage("com.gem.keeppeer");
        require(intent!=null,"Launcher missing");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getTargetContext().startActivity(intent);
    }
    private void hideKeyboard() {
        try { shell("input keyevent 4"); } catch (Exception ignored) {}
        SystemClock.sleep(350);
    }
    private void setup() {
        launch(); await("Create your vault",60000); SystemClock.sleep(500);
        field("Master password",MASTER); field("Confirm master password",MASTER);
        // Temporary Part B diagnostics; no authentication or timeout changes.
        Bundle preHide=new Bundle();
        try { preHide.putString("phase5PreHideIme",shell("dumpsys input_method | grep mInputShown")); }
        catch (IOException failure) { preHide.putString("phase5PreHideIme","Diagnostic unavailable: IOException"); }
        sendStatus(2,preHide);
        hideKeyboard();
        Bundle postHide=new Bundle();
        try { postHide.putString("phase5PostHideIme",shell("dumpsys input_method | grep mInputShown")); }
        catch (IOException failure) { postHide.putString("phase5PostHideIme","Diagnostic unavailable: IOException"); }
        sendStatus(2,postHide);
        action("Create vault"); await("Add",120000); checkpoint("fresh-setup");
    }
    private void unlock(String password) {
        await("Keeppeer locked",120000); field("Master password",password);
        hideKeyboard();
        click("Unlock"); await("Add",120000);
    }
    private void settings() { click("Security"); await("Security settings"); }
    private void checkpoint(String name) {
        stage=name; Bundle values=new Bundle(); values.putString("phase5Checkpoint",name); sendStatus(2,values);
    }
    private boolean checked(String label) {
        AccessibilityNodeInfo node=matching(label);
        for (int depth=0; node!=null && depth<12; depth++,node=node.getParent()) {
            if (node.isCheckable()) return node.isChecked();
            if (node.isSelected()) return true;
        }
        return false;
    }
    private void coldReopen() {
        launch(); unlock(CHANGED); await(TITLE);
        settings(); scrollTo("Dark");
        long end=SystemClock.elapsedRealtime()+10000;
        while (!checked("Dark") && SystemClock.elapsedRealtime()<end) SystemClock.sleep(100);
        require(checked("Dark"),"Saved Dark appearance must survive a new process");
        click("Back"); await(TITLE);
        checkpoint("ordinary-cold-entry-and-persisted-dark");
    }
    private void compatibility() {
        setup(); click("Add"); field("Title *",TITLE); field("Password","synthetic API compatibility");
        action("Save"); await("Edit"); click("Back"); await(TITLE); settings(); action("Lock now");
        unlock(MASTER); await(TITLE); checkpoint("minified-normal-portable-KDF-native-SQLCipher-lock-reopen");
    }
    private void whole() {
        setup(); click("Add"); field("Title *",TITLE); field("Password","  =synthetic,\"exact\"  ");
        action("Generate…"); await("Generate password"); click("Cancel");
        action("Generate…"); click("Use password (replace draft)"); action("Save"); await("Edit"); click("Back"); await(TITLE);
        checkpoint("generator-cancel-use-and-create");
        click("Add to favorites"); await("Favorites");
        field("Search title, username, URL or category","no matching synthetic value");
        await("No matching entries. Try clearing filters.");
        field("Search title, username, URL or category",""); await(TITLE);
        checkpoint("favorite-and-search");
        click(TITLE); await("Edit"); click("Back"); settings(); action("Dark"); click("Back");
        settings(); action("Lock now"); await("Keeppeer locked"); click("Unlock"); await(WRONG,120000);
        field("Master password","incorrect synthetic master"); click("Unlock"); await(WRONG,120000);
        unlock(MASTER); await(TITLE); checkpoint("empty-wrong-password-lock-reopen");
        settings(); action("Export CSV"); await("This file contains your passwords in plain text. Store it somewhere safe."); click("Continue");
        long end=SystemClock.elapsedRealtime()+30000; AccessibilityNodeInfo filename=null;
        while(SystemClock.elapsedRealtime()<end && filename==null) {
            for(AccessibilityNodeInfo node:nodes()) if(node.isEditable() && String.valueOf(node.getPackageName()).contains("documentsui")) { filename=node; break; }
            if(filename==null) SystemClock.sleep(150);
        }
        require(filename!=null,"Real SAF filename unavailable");
        Bundle name=new Bundle(); name.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"keeppeer-phase5-synthetic.csv");
        require(filename.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,name),"SAF filename failed");
        click("SAVE"); unlock(MASTER);
        await("This file contains your passwords in plain text. Store it somewhere safe."); click("Continue");
        await("CSV export finished. Protect the plaintext file separately."); click("Continue");
        checkpoint("real-SAF-export-immediate-lock-renewed-approval");
        click(TITLE); action("Delete"); await("Delete entry?"); click("Delete");
        await("No entries yet. Add an entry to get started.");
        settings(); action("Import CSV"); click("keeppeer-phase5-synthetic.csv"); unlock(MASTER); click("Continue");
        await("Total: 1 · New: 1 · Conflicting: 0"); click("Continue");
        await("Import finished: 1 entry inserted, 0 entries updated, 0 entries skipped."); click("Continue"); await(TITLE);
        checkpoint("real-SAF-restore-after-delete");
        settings(); scrollTo("Current master password"); field("Current master password",MASTER);
        scrollTo("New master password"); field("New master password",CHANGED);
        scrollTo("Confirm new password"); field("Confirm new password",CHANGED);
        action("Change password"); await("Master password changed. Existing entries and database key are unchanged.",120000);
        click("Back"); settings(); action("Lock now"); unlock(CHANGED); await(TITLE); checkpoint("password-change-preserves-entry");
    }
    private void seedLegacy() throws IOException {
        require(!getTargetContext().getDatabasePath("keeppeer_vault.db").exists(),"Fresh legacy fixture required");
        File file=getTargetContext().getDatabasePath("keeppeer_prototype.db");
        file.getParentFile().mkdirs(); require(file.createNewFile(),"Never overwrite existing fixture");
        try(OutputStream out=new FileOutputStream(file)) { out.write("Phase5 synthetic legacy preservation fixture".getBytes("UTF-8")); }
        checkpoint("legacy-seeded-restart-required");
    }
    private void refuseLegacy() throws IOException {
        launch(); await("Vault unavailable");
        require(matching("Reset synthetic development data…")==null,"Reset must not be exposed");
        require(matching("Create vault")==null && matching("Unlock")==null,"Unsafe legacy control exposed");
        File file=getTargetContext().getDatabasePath("keeppeer_prototype.db");
        byte[] expected="Phase5 synthetic legacy preservation fixture".getBytes("UTF-8");
        byte[] actual=new byte[expected.length];
        try(InputStream input=new FileInputStream(file)) { require(input.read(actual)==actual.length && input.read()==-1,"Legacy length changed"); }
        require(Arrays.equals(expected,actual),"Legacy bytes changed");
        require(!getTargetContext().getDatabasePath("keeppeer_vault.db").exists(),"Unexpected new vault");
        checkpoint("minified-legacy-refusal-preserves-data");
    }
}
