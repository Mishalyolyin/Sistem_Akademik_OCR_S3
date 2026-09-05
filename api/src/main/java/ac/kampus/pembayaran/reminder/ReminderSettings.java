package ac.kampus.pembayaran.reminder;

/** Kunci pengaturan pengingat di {@code system_settings}. */
public final class ReminderSettings {

	public static final String ENABLED = "reminder_enabled";
	public static final String DAYS_BEFORE = "reminder_days_before";
	public static final String OVERDUE_ENABLED = "reminder_overdue_enabled";
	public static final String GATEWAY_URL = "whatsapp_gateway_url";
	public static final String GATEWAY_TOKEN = "whatsapp_gateway_token";
	public static final String MESSAGE_TEMPLATE = "reminder_message_template";

	/** Dipakai bila admin belum mengatur berapa hari sebelum jatuh tempo. */
	public static final int DAYS_BEFORE_DEFAULT = 3;

	private ReminderSettings() {
	}
}
