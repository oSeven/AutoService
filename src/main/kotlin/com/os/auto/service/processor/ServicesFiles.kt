package com.os.auto.service.processor

import com.google.common.io.Closer
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import kotlin.text.Charsets.UTF_8

/**
 *
 * Created on 2018/8/25.
 *
 * @author o.s
 */
object ServicesFiles {
    private const val SERVICES_PATH = "META-INF/services"


    /**
     * Returns an absolute path to a service file given the class
     * name of the service.
     *
     * @param serviceName not `null`
     * @return SERVICES_PATH + serviceName
     */
    fun getPath(serviceName: String): String {
        return "$SERVICES_PATH/$serviceName"
    }

    /**
     * Reads the set of service classes from a service file.
     *
     * @param input not `null`. Closed after use.
     * @return a not `null Set` of service class names.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun readServiceFile(input: InputStream): Set<String> {
        val serviceClasses = HashSet<String>()
        val closer = Closer.create()
        try {
            val r = closer.register(BufferedReader(InputStreamReader(input, UTF_8)))
            var line: String? = r.readLine()
            while (line != null) {
                val commentStart = line.indexOf('#')
                if (commentStart >= 0) {
                    line = line.substring(0, commentStart)
                }
                line = line.trim { it <= ' ' }
                if (!line.isEmpty()) {
                    serviceClasses.add(line)
                }
                line = r.readLine()
            }
            return serviceClasses
        } catch (t: Throwable) {
            throw closer.rethrow(t)
        } finally {
            closer.close()
        }
    }

    /**
     * Writes the set of service class names to a service file.
     *
     * @param output not `null`. Not closed after use.
     * @param services a not `null Collection` of service class names.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun writeServiceFile(services: Collection<String>, output: OutputStream) {
        val writer = BufferedWriter(OutputStreamWriter(output, UTF_8))
        for (service in services) {
            writer.write(service)
            writer.newLine()
        }
        writer.flush()
    }
}