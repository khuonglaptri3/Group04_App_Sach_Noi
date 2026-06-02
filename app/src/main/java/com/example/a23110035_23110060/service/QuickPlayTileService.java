package com.example.a23110035_23110060.service;

import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.a23110035_23110060.controller.PlayerManager;
import com.example.a23110035_23110060.controller.SessionManager;
import com.example.a23110035_23110060.view.activity.MainActivity;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickPlayTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        
        PlayerManager playerManager = PlayerManager.getInstance();
        if (playerManager.getCurrentBook() != null) {
            // Trình phát đang chạy hoặc đang tạm dừng trong bộ nhớ
            playerManager.togglePlayPause();
            updateTileState();
        } else {
            // App đã bị tắt hoặc trình phát trống, mở lại app với sách cuối cùng
            SessionManager sessionManager = new SessionManager(this);
            String lastBookId = sessionManager.getLastPlayedBookId();
            
            if (lastBookId != null) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setAction("ACTION_RESUME_LAST_BOOK");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivityAndCollapse(intent);
            }
        }
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile != null) {
            PlayerManager playerManager = PlayerManager.getInstance();
            if (playerManager.getCurrentBook() != null && playerManager.isPlaying()) {
                tile.setState(Tile.STATE_ACTIVE);
                tile.setLabel("Đang phát");
            } else {
                tile.setState(Tile.STATE_INACTIVE);
                tile.setLabel("Tiếp tục nghe");
            }
            tile.updateTile();
        }
    }
}
