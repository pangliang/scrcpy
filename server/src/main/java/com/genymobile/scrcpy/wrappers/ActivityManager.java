package com.genymobile.scrcpy.wrappers;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import com.genymobile.scrcpy.Ln;

public class ActivityManager {

    private final IInterface manager;
    private Method getContentProviderExternalMethod;
    private boolean getContentProviderExternalMethodLegacy;
    private Method removeContentProviderExternalMethod;
    private Method broadcastIntentMethod;
    private Method registerReceiverMethod;

    public ActivityManager(IInterface manager) {
        this.manager = manager;
        try {
            for (Method method : manager.getClass().getDeclaredMethods()) {
                if (method.getName().equals("broadcastIntent")) {
                    int parameterLength = method.getParameterTypes().length;
                    if (parameterLength != 13 && parameterLength != 12 && parameterLength != 11) {
                        Ln.i("broadcastIntent method parameter length wrong.");
                        continue;
                    }
                    broadcastIntentMethod = method;
                }else if(method.getName().equals("registerReceiver")) {
                    registerReceiverMethod = method;
                }
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private Method getGetContentProviderExternalMethod() throws NoSuchMethodException {
        if (getContentProviderExternalMethod == null) {
            try {
                getContentProviderExternalMethod = manager.getClass()
                        .getMethod("getContentProviderExternal", String.class, int.class, IBinder.class, String.class);
            } catch (NoSuchMethodException e) {
                // old version
                getContentProviderExternalMethod = manager.getClass().getMethod("getContentProviderExternal", String.class, int.class, IBinder.class);
                getContentProviderExternalMethodLegacy = true;
            }
        }
        return getContentProviderExternalMethod;
    }

    private Method getRemoveContentProviderExternalMethod() throws NoSuchMethodException {
        if (removeContentProviderExternalMethod == null) {
            removeContentProviderExternalMethod = manager.getClass().getMethod("removeContentProviderExternal", String.class, IBinder.class);
        }
        return removeContentProviderExternalMethod;
    }

    private ContentProvider getContentProviderExternal(String name, IBinder token) {
        try {
            Method method = getGetContentProviderExternalMethod();
            Object[] args;
            if (!getContentProviderExternalMethodLegacy) {
                // new version
                args = new Object[]{name, ServiceManager.USER_ID, token, null};
            } else {
                // old version
                args = new Object[]{name, ServiceManager.USER_ID, token};
            }
            // ContentProviderHolder providerHolder = getContentProviderExternal(...);
            Object providerHolder = method.invoke(manager, args);
            if (providerHolder == null) {
                return null;
            }
            // IContentProvider provider = providerHolder.provider;
            Field providerField = providerHolder.getClass().getDeclaredField("provider");
            providerField.setAccessible(true);
            Object provider = providerField.get(providerHolder);
            if (provider == null) {
                return null;
            }
            return new ContentProvider(this, provider, name, token);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException | NoSuchFieldException e) {
            Ln.e("Could not invoke method", e);
            return null;
        }
    }

    void removeContentProviderExternal(String name, IBinder token) {
        try {
            Method method = getRemoveContentProviderExternalMethod();
            method.invoke(manager, name, token);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            Ln.e("Could not invoke method", e);
        }
    }

    public ContentProvider createSettingsProvider() {
        return getContentProviderExternal("settings", new Binder());
    }

    public void sendBroadcast(Intent paramIntent) {
        try {
            if (broadcastIntentMethod.getParameterTypes().length == 11) {
                broadcastIntentMethod.invoke(
                    manager, null, paramIntent, null, null, 0, null, null, null, Boolean.TRUE, Boolean.FALSE, -2);
            } else if (broadcastIntentMethod.getParameterTypes().length == 12) {
                broadcastIntentMethod.invoke(
                    manager, null, paramIntent, null, null, 0, null, null, null, -1, Boolean.TRUE, Boolean.FALSE, -2);
            } else if (broadcastIntentMethod.getParameterTypes().length == 13) {
                broadcastIntentMethod.invoke(
                    manager, null, paramIntent, null, null, 0, null, null, null, -1, null, Boolean.TRUE, Boolean.FALSE, -2);
            }
        }catch (Exception e){
            throw new AssertionError(e);
        }
    }

    //Currently not working
    public Intent registerReceiver(IIntentReceiver receiver, IntentFilter intentFilter) {
        try {
            return (Intent)registerReceiverMethod.invoke(manager, null, null, receiver, intentFilter, null, -2, 0);
        }catch (Exception e){
            throw new AssertionError(e);
        }
    }
}
