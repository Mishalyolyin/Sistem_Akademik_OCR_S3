package ac.kampus.pembayaran.common;

/** Entitas yang diminta tidak ada. Dipetakan ke HTTP 404. */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}

	public static NotFoundException of(String entity, Object id) {
		return new NotFoundException("%s dengan id %s tidak ditemukan.".formatted(entity, id));
	}
}
