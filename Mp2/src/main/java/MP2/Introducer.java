public enum Introducer
{
    IPADDRESS("ipaddress", "172.22.154.195"),
    PORT("port", "5000");

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
