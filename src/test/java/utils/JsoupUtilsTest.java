package utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class JsoupUtilsTest {
    private static final String URL = "https://example.test/sticker/123/android/sticker.png";

    private static String[] imageLinks(String... styles) {
        Document doc = Jsoup.parse("<div class='mdCMN09ImgList'><ul class='mdCMN09Ul'></ul></div>");
        for (String style : styles) {
            doc.select("ul").first().appendElement("li").addClass("mdCMN09Li")
                    .appendElement("span").addClass("mdCMN09Image").attr("style", style);
        }
        return JsoupUtils.INSTANCE.getImageLinks(doc);
    }

    @Test public void acceptsUrlWithoutImageParameters() {
        assertArrayEquals(new String[]{URL}, imageLinks("background-image: url(" + URL + ");"));
    }

    @Test public void acceptsUrlWithoutDeclarationSemicolon() {
        assertArrayEquals(new String[]{URL}, imageLinks("background-image: url(" + URL + ")"));
    }

    @Test public void acceptsSingleQuotedUrl() {
        assertArrayEquals(new String[]{URL}, imageLinks("background-image: url('" + URL + "');"));
    }

    @Test public void acceptsDoubleQuotedUrl() {
        assertArrayEquals(new String[]{URL}, imageLinks("background-image: url(\"" + URL + "\");"));
    }

    @Test public void removesWhitespaceOutsideUrl() {
        assertArrayEquals(new String[]{URL}, imageLinks("background-image: url( \t" + URL + " \t);"));
    }

    @Test public void removesOnlyOneMatchingPairOfCssQuotes() {
        assertArrayEquals(new String[]{"'sticker.png'"}, imageLinks("background-image: url(\"'sticker.png'\");"));
    }

    @Test public void preservesLegacyImageParameterRemoval() {
        assertArrayEquals(new String[]{URL}, imageLinks("background-image: url(" + URL + ";compress=true);"));
    }

    @Test public void removesParametersFromSingleQuotedUrl() {
        assertArrayEquals(new String[]{URL}, imageLinks("background-image: url( '" + URL + ";compress=true' );"));
    }

    @Test public void removesParametersFromDoubleQuotedUrl() {
        assertArrayEquals(new String[]{URL}, imageLinks("background-image: url( \"" + URL + ";compress=true\" );"));
    }

    @Test public void preservesQueryAndFragmentWithoutImageParameters() {
        String url = URL + "?variant=original&v=2#image";
        assertArrayEquals(new String[]{url}, imageLinks("background-image: url('" + url + "');"));
    }

    @Test public void preservesOrderAndExistingLastParameterSemantics() {
        String other = URL.replace("/123/", "/456/");
        assertArrayEquals(new String[]{URL + ";first=1", other}, imageLinks(
                "background-image: url(" + URL + ";first=1;compress=true);",
                "background-image: url(" + other + ";compress=true);"));
    }
}
