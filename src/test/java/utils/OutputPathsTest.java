package utils;

import java.awt.Dimension;
import java.lang.reflect.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.BeforeClass;
import org.junit.Test;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import static org.junit.Assert.*;

public class OutputPathsTest {
    private static Path root;
    @BeforeClass public static void setup() throws Exception {
        root = Files.createDirectory(Files.createTempDirectory("sticker-output-tests-").resolve("working"));
        System.setProperty("user.dir", root.toString());
        System.setProperty("java.awt.headless", "true");
    }
    private static String title(String text) {
        Document doc = Jsoup.parse("<div class='mdBox03Inner01'><p></p></div>");
        doc.select("p").first().text(text);
        return JsoupUtils.INSTANCE.getPackTitle(doc);
    }
    private static void rejected(Runnable action) {
        try { action.run(); fail("Expected unsafe output path rejection"); }
        catch (IllegalArgumentException expected) { }
    }
    @Test public void rejectsTraversalAcrossAllSinksBeforeReadingImage() {
        String[] unsafe = {"../outside", "a/../../outside", "..\\outside", "\\\\server\\share", "/absolute", "C:\\outside", "a/b", "a\\b", ".", "..", "", "bad\u0000name", "bad\u0085name", "name:stream", "bad?name", "bad*name", "name.", "name ", "CON", "nul.png", "LPT1", "COM¹"};
        for (String name : unsafe) {
            rejected(() -> Utils.INSTANCE.createDirectory(name));
            rejected(() -> Utils.INSTANCE.saveImage("not-a-url", "123", name));
            rejected(() -> Utils.INSTANCE.writeCaptionImage("fixture", name));
        }
        assertFalse(Files.exists(root.getParent().resolve("outside")));
    }
    @Test public void rejectsHtmlTitleTraversalWithoutReinterpretingIt() {
        String name = title("../outside");
        assertEquals("../outside", name);
        rejected(() -> Utils.INSTANCE.createDirectory(name));
        rejected(() -> Utils.INSTANCE.writeCaptionImage("fixture", name));
    }
    @Test public void preservesUnicodeAndOrdinaryNamesAndWritesOfflineImages() throws Exception {
        String name = title("貓咪 café 🐈 v1.2");
        assertEquals("貓咪 café 🐈 v1.2", name);
        Utils.INSTANCE.createDirectory(name);
        Utils.INSTANCE.createDirectory(name);
        Path input = root.resolve("fixture.png");
        ImageIO.write(new BufferedImage(8, 4, BufferedImage.TYPE_INT_ARGB), "png", input.toFile());
        Utils.INSTANCE.saveImage(input.toUri().toString(), "12345678901234567890", name);
        Utils.INSTANCE.writeCaptionImage("offline fixture", name);
        assertEquals(512, ImageIO.read(root.resolve(name).resolve("12345678901234567890.png").toFile()).getWidth());
        assertNotNull(ImageIO.read(root.resolve(name).resolve("copyright.png").toFile()));
        Utils.INSTANCE.createDirectory(".cute");
        Utils.INSTANCE.createDirectory("%2e%2e%2foutside"); // Literal text, never URL decoded.
        assertTrue(Files.isDirectory(root.resolve(".cute")));
        assertTrue(Files.isDirectory(root.resolve("%2e%2e%2foutside")));
    }
    private static void assertResizedImage(int width, int height, int expectedWidth, int expectedHeight) throws Exception {
        String name = "resize-" + width + "x" + height;
        Path input = root.resolve(name + ".png");
        BufferedImage fixture = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) fixture.setRGB(x, y, 0xffff00ff);
        }
        assertTrue(ImageIO.write(fixture, "png", input.toFile()));
        Utils.INSTANCE.createDirectory("resized-images");
        Utils.INSTANCE.saveImage(input.toUri().toString(), name, "resized-images");
        BufferedImage output = ImageIO.read(root.resolve("resized-images").resolve(name + ".png").toFile());
        assertNotNull(output);
        assertEquals(expectedWidth, output.getWidth());
        assertEquals(expectedHeight, output.getHeight());
        assertEquals(0xffff00ff, output.getRGB(output.getWidth() / 2, output.getHeight() / 2));
    }
    @Test public void resizesThinLandscapeImagesWithoutZeroHeight() throws Exception {
        assertResizedImage(513, 1, 512, 1);
        assertResizedImage(1024, 1, 512, 1);
    }
    @Test public void resizesThinPortraitImagesWithoutZeroWidth() throws Exception {
        assertResizedImage(1, 513, 1, 512);
        assertResizedImage(1, 1024, 1, 512);
    }
    @Test public void preservesOnePixelAndExactSizeBoundaries() throws Exception {
        assertResizedImage(512, 1, 512, 1);
        assertResizedImage(1, 512, 1, 512);
        assertResizedImage(1, 1, 512, 512);
        assertResizedImage(512, 512, 512, 512);
    }
    @Test public void preservesOrdinaryResizeProportions() throws Exception {
        assertResizedImage(320, 160, 512, 256);
        assertResizedImage(160, 320, 256, 512);
        assertResizedImage(1024, 512, 512, 256);
        assertResizedImage(512, 1024, 256, 512);
    }
    @Test public void keepsNegativeOverflowDimensionsInvalid() throws Exception {
        // Report extreme dimensions without allocating or rendering a large image.
        BufferedImage fixture = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB) {
            @Override public int getHeight() { return 4194304; }
        };
        Method scaledDimension = Utils.class.getDeclaredMethod("getScaledDimension", BufferedImage.class);
        scaledDimension.setAccessible(true);
        Dimension dimension = (Dimension) scaledDimension.invoke(Utils.INSTANCE, fixture);
        assertEquals(512, dimension.width);
        assertEquals(Integer.MIN_VALUE, dimension.height);
        try {
            new BufferedImage(dimension.width, dimension.height, BufferedImage.TYPE_INT_ARGB);
            fail("Expected negative overflow dimension rejection");
        } catch (IllegalArgumentException expected) { }
    }
    @Test public void defaultOverloadsRemainConfinedAndImageNamesAreChecked() throws Exception {
        Utils.INSTANCE.createDirectory("stickers");
        Utils.INSTANCE.writeCaptionImage("default fixture");
        assertTrue(Files.exists(root.resolve("stickers/copyright.png")));
        Path input = root.resolve("default-fixture.png");
        ImageIO.write(new BufferedImage(4, 8, BufferedImage.TYPE_INT_ARGB), "png", input.toFile());
        Utils.INSTANCE.saveImage(input.toUri().toString(), "42");
        assertEquals(512, ImageIO.read(root.resolve("stickers/42.png").toFile()).getHeight());
        rejected(() -> Utils.INSTANCE.saveImage("not-a-url", "../../outside"));
        rejected(() -> Utils.INSTANCE.saveImage("not-a-url", "..\\outside", "stickers"));
        rejected(() -> Utils.INSTANCE.saveImage("not-a-url", "file:stream", "stickers"));
    }
    @Test public void normalizedPathsHaveImmediateExpectedParents() {
        OutputPaths paths = new OutputPaths(root.resolve(".").resolve("unused").resolve(".."));
        assertEquals(root, paths.directory("pack").getParent());
        assertEquals(root.resolve("pack"), paths.file("pack", "123.png").getParent());
    }
    @Test public void rejectsExistingNonDirectory() throws Exception {
        Files.write(root.resolve("not-directory"), new byte[]{1});
        rejected(() -> Utils.INSTANCE.createDirectory("not-directory"));
        rejected(() -> Utils.INSTANCE.writeCaptionImage("fixture", "not-directory"));
    }
    @Test public void rejectsStaticDirectoryAndFileSymlinks() throws Exception {
        Path outside = Files.createTempDirectory("sticker-outside-target-");
        Files.createSymbolicLink(root.resolve("linked-pack"), outside);
        rejected(() -> Utils.INSTANCE.createDirectory("linked-pack"));
        rejected(() -> Utils.INSTANCE.saveImage("not-a-url", "123", "linked-pack"));
        rejected(() -> Utils.INSTANCE.writeCaptionImage("fixture", "linked-pack"));
        Utils.INSTANCE.createDirectory("symlink-files");
        Path sentinel = Files.write(outside.resolve("sentinel"), new byte[]{1, 2, 3});
        Files.createSymbolicLink(root.resolve("symlink-files/copyright.png"), sentinel);
        Files.createSymbolicLink(root.resolve("symlink-files/123.png"), sentinel);
        rejected(() -> Utils.INSTANCE.writeCaptionImage("fixture", "symlink-files"));
        rejected(() -> Utils.INSTANCE.saveImage("not-a-url", "123", "symlink-files"));
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(sentinel));
    }
}
