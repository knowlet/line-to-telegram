package utils

import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.util.Locale

/** Confines untrusted pack titles and file names to one output directory. */
internal class OutputPaths(root: Path) {
    private val root = root.toAbsolutePath().normalize().toRealPath()

    private fun component(name: String): String {
        require(name.isNotEmpty() && name != "." && name != ".." &&
                !name.endsWith(".") && !name.endsWith(" ") &&
                name.none { Character.isISOControl(it) || it in "<>:\"/\\|?*" }) {
            "Output names must be safe, non-empty single path components"
        }
        val stem = name.substringBefore('.').toUpperCase(Locale.ROOT)
        require(stem !in setOf("CON", "PRN", "AUX", "NUL") &&
                !stem.matches(Regex("(?:COM|LPT)[1-9¹²³]"))) {
            "Reserved output name"
        }
        return name
    }

    fun directory(name: String): Path {
        val path = root.resolve(component(name)).normalize()
        require(path.parent == root && path.startsWith(root)) { "Output directory escapes its root" }
        require(!Files.isSymbolicLink(path)) { "Output directory must not be a symbolic link" }
        if (Files.exists(path, NOFOLLOW_LINKS)) {
            require(Files.isDirectory(path, NOFOLLOW_LINKS)) { "Output path is not a directory" }
            require(path.toRealPath() == path) { "Output directory must not redirect elsewhere" }
        }
        return path
    }

    fun file(directory: String, name: String): Path {
        val dir = directory(directory)
        val path = dir.resolve(component(name)).normalize()
        require(path.parent == dir && path.startsWith(root)) { "Output file escapes its directory" }
        require(!Files.isSymbolicLink(path)) { "Output file must not be a symbolic link" }
        return path
    }
}
