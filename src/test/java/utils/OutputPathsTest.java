package utils;

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
