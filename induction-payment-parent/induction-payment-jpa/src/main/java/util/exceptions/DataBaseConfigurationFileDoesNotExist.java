package util.exceptions;

public class DataBaseConfigurationFileDoesNotExist extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DataBaseConfigurationFileDoesNotExist(String e) {
	super(e);
    }
}
