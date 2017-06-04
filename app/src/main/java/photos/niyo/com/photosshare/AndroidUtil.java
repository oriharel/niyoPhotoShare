package photos.niyo.com.photosshare;

public class AndroidUtil {

    public static String getArrayAsString(Object[] array)
    {
        String result = "";
        if (array != null) {
            for (Object object : array) {
                result += object.toString() + ", ";
            }
        }
        return result;
    }
}
