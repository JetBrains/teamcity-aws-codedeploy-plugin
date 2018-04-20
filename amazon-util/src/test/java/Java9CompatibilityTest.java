import com.amazonaws.util.Base64;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class Java9CompatibilityTest {
    @Test
    public void Base64WorksWithoutJAXB() throws Throwable {
        final byte[] from = "test".getBytes("UTF-8");
        final String encoded = Base64.encodeAsString(from);
        Assert.assertEquals(encoded, "dGVzdA==");
        final byte[] decode = Base64.decode(encoded);
        Assert.assertEquals(decode, from);
    }
}
