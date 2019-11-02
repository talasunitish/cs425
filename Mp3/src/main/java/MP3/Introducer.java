/**
 * Enum for introducer IpAddress and port to send join messages.
 */
public enum Introducer
{

    IPADDRESS("ipaddress", "127.0.0.1"),
    PORT("port", "5500");

    private final String key;
    private final String value;

    Introducer(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }
    public String getValue() {
        return value;
    }
}