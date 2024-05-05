import org.archivekeep.core.exceptions.MaliciousPath
import org.archivekeep.core.exceptions.NotNormalizedPath
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.readBytes


internal fun safeSubPath(path: String): Path {
    return safeSubPath(Paths.get(path))
}

internal fun safeSubPath(path: Path): Path {
    val p = path.normalize()
    val s = p.invariantSeparatorsPathString

    if( s == ".." || s.startsWith("../")) {
        throw MaliciousPath(path.toString())
    }

    if(p.invariantSeparatorsPathString != path.toString()) {
        throw NotNormalizedPath(path.toString())
    }

    return p
}

internal fun computeChecksum(path: Path): String {
    val md = MessageDigest.getInstance("SHA-256")

    val content = path.readBytes()
    val digest = md.digest(content)

    return digest.joinToString("") { "%02x".format(it) }

}
