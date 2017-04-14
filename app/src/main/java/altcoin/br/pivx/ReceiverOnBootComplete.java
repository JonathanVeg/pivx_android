package altcoin.br.pivx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import altcoin.br.pivx.services.PriceAlertService;

public class ReceiverOnBootComplete extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            context.startService(new Intent(context, PriceAlertService.class));
        }
    }
}
