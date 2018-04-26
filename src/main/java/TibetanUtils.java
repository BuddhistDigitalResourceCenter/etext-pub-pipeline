public class TibetanUtils {

    private static final String tibetanNumbers = "༠༡༢༣༤༥༦༧༨༩";

    public static String getTibetanNumber(int number)
    {
        String tibetanNumber = "";
        char[] numberChars = Integer.toString(number).toCharArray();
        for (char numberChar: numberChars) {
            tibetanNumber += tibetanNumbers.charAt(Integer.parseInt(String.valueOf(numberChar)));
        }

        return tibetanNumber;
    }
}
