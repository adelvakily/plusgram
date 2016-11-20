package org.telbit;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import org.plusgram.messenger.R;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.BaseFragment;

public class CallsManager
{
    private static final String TELBIT_PACKAGE_NAME = "org.telbit";
    private static final String TELBIT_SERVICE_PACKAGE_NAME = "org.telbit.services.RemoteService";
    private static final String TELBIT_DOWNLOAD_ADDRESS = "http://www.telbit.org/dl";

    private static CallsManager INSTANCE;

    public static synchronized CallsManager getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new CallsManager();
            INSTANCE.init(ApplicationLoader.applicationContext);
        }

        return INSTANCE;
    }

    private IRemoteAccess remoteAccess;
    private boolean bound;


    private CallsManager() {}

    public void init(Context context)
    {
        if (!bound)
        {
            try
            {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(TELBIT_PACKAGE_NAME, TELBIT_SERVICE_PACKAGE_NAME));
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            }
            catch (Exception ignored) {}
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder)
        {
            remoteAccess = IRemoteAccess.Stub.asInterface(iBinder);
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            bound = false;
            remoteAccess = null;
        }
    };

    public void requestCall(final BaseFragment fragment, String phoneNumber)
    {
        if (fragment.getParentActivity() == null)
            return;

        if (!isTelBitInstalled())
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
            builder.setTitle(R.string.TelBitNotFoundTitle);
            builder.setMessage(R.string.InstallTelBitMessage);
            builder.setNegativeButton(R.string.Install, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    Browser.openUrl(fragment.getParentActivity(), TELBIT_DOWNLOAD_ADDRESS);
                }
            });

            builder.show();
            return;
        }

        if (!bound)
            init(fragment.getParentActivity());

        if (bound && remoteAccess != null)
        {
            try
            {
                remoteAccess.requestCall(phoneNumber);
            }
            catch (Exception ignored) {}
        }
    }

    private boolean isTelBitInstalled()
    {
        try
        {
            PackageManager packageManager = ApplicationLoader.applicationContext.getPackageManager();
            PackageInfo info = packageManager.getPackageInfo(TELBIT_PACKAGE_NAME, PackageManager.GET_META_DATA);
            if (info != null)
                return true;
        }
        catch (Exception ignored) {}

        return false;
    }
}
