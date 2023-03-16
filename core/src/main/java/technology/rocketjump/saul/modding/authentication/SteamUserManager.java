package technology.rocketjump.saul.modding.authentication;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.utils.Disposable;
import com.codedisaster.steamworks.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.codec.binary.Base64;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;

import java.nio.ByteBuffer;

@Singleton
public class SteamUserManager implements SteamUserCallback, Disposable {

	private final MessageDispatcher messageDispatcher;
	private SteamUser steamUser;
	private ByteBuffer ticketEncrypted = ByteBuffer.allocateDirect(1024);
	private boolean isEncryptedAppTicketReady = false;
	private int ticketSize;

	@Inject
	public SteamUserManager(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
		if (SteamAPI.isSteamRunning()) {
			if (GlobalSettings.DEV_MODE) {
				Logger.debug("Steam is running, requesting encrypted app ticket");
			}
			steamUser = new SteamUser(this);

			ByteBuffer empty = ByteBuffer.allocateDirect(0);
			try {
				steamUser.requestEncryptedAppTicket(empty);
			} catch (SteamException e) {
				if (GlobalSettings.DEV_MODE) {
					Logger.error(e, "Error requesting Steam encrypted app ticket");
				}
			}
		} else if (GlobalSettings.DEV_MODE) {
			Logger.debug("Steam is not running, not requesting encrypted app ticket");
		}
	}

	@Override
	public void dispose() {
		if (steamUser != null) {
			steamUser.dispose();
			steamUser = null;
		}
	}

	@Override
	public void onEncryptedAppTicket(SteamResult result) {
		if (result == SteamResult.OK) {
			int[] ticketSize = new int[1];
			try {
				steamUser.getEncryptedAppTicket(ticketEncrypted, ticketSize);
				Logger.info("Received Steam encrypted app ticket: {}", new String(ticketEncrypted.array()));
				Logger.info("As base64 encoded: " + Base64.encodeBase64String(ticketEncrypted.array()));
				isEncryptedAppTicketReady = true;
				this.ticketSize = ticketSize[0];
				messageDispatcher.dispatchMessage(MessageType.STEAM_ENCRYPTED_APP_TICKET_READY);
			} catch (SteamException e) {
				if (GlobalSettings.DEV_MODE) {
					Logger.error(e, "Error getting Steam encrypted app ticket");
				}
			}
		} else {
			if (GlobalSettings.DEV_MODE) {
				Logger.error("Error requesting Steam encrypted app ticket: {}", result);
			}
		}
	}

	@Override
	public void onAuthSessionTicket(SteamAuthTicket authTicket, SteamResult result) {

	}

	@Override
	public void onValidateAuthTicket(SteamID steamID, SteamAuth.AuthSessionResponse authSessionResponse, SteamID ownerSteamID) {

	}

	@Override
	public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {

	}

	public boolean isEncryptedAppTicketReady() {
		return isEncryptedAppTicketReady;
	}

	public ByteBuffer getEncryptedAppTicket() {
		return ticketEncrypted;
	}
}
