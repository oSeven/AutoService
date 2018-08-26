package com.os.auto.service.processor

import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import com.os.auto.service.AutoService
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation

/**
 *
 * Created on 2018/8/25.
 *
 * @author o.s
 */
class AutoServiceProcessor : AbstractProcessor() {

    private val processorFileName = "javax.annotation.processing.Processor"
    private val providers = HashMultimap.create<String, String>()

    override fun getSupportedAnnotationTypes(): ImmutableSet<String> {
        return ImmutableSet.of(AutoService::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        return try {
            processImpl(annotations, roundEnv)
        } catch (e: Exception) {
            // We don't allow exceptions of any kind to propagate to the compiler
            val writer = StringWriter()
            e.printStackTrace(PrintWriter(writer))
            fatalError(writer.toString())
            true
        }

    }

    private fun processImpl(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (roundEnv.processingOver()) {
            generateConfigFiles()
        } else {
            processAnnotations(annotations, roundEnv)
        }
        return true
    }

    private fun processAnnotations(annotations: Set<TypeElement>,
                                   roundEnv: RoundEnvironment) {

        val elements = roundEnv.getElementsAnnotatedWith(AutoService::class.java)

        log("annotations :: $annotations")
        log("element :: $elements")

        for (e in elements) {
            log("e :: $e")
            providers.put(processorFileName, e.toString())
        }
    }

    private fun generateConfigFiles() {
        val filer = processingEnv.filer

        for (providerInterface in providers.keySet()) {
            val resourceFile = "META-INF/services/$providerInterface"
            log("Working on resource file: $resourceFile")
            try {
                val allServices = Sets.newTreeSet<String>()
                try {
                    // would like to be able to print the full path
                    // before we attempt to get the resource in case the behavior
                    // of filer.getResource does change to match the spec, but there's
                    // no good way to resolve CLASS_OUTPUT without first getting a resource.
                    val existingFile = filer.getResource(StandardLocation.CLASS_OUTPUT, "",
                            resourceFile)
                    log("Looking for existing resource file at " + existingFile.toUri())
                    val oldServices = ServicesFiles.readServiceFile(existingFile.openInputStream())
                    log("Existing service entries: $oldServices")
                    allServices.addAll(oldServices)
                } catch (e: IOException) {
                    // According to the javadoc, Filer.getResource throws an exception
                    // if the file doesn't already exist.  In practice this doesn't
                    // appear to be the case.  Filer.getResource will happily return a
                    // FileObject that refers to a non-existent file but will throw
                    // IOException if you try to open an input stream for it.
                    log("Resource file did not already exist.")
                }

                val newServices = HashSet<String>(providers.get(providerInterface))
                if (allServices.containsAll(newServices)) {
                    log("No new service entries being added.")
                    return
                }

                allServices.addAll(newServices)
                log("New service file contents: $allServices")
                val fileObject = filer.createResource(StandardLocation.CLASS_OUTPUT, "",
                        resourceFile)
                val out = fileObject.openOutputStream()
                ServicesFiles.writeServiceFile(allServices, out)
                out.close()
                log("Wrote to: " + fileObject.toUri())
            } catch (e: IOException) {
                fatalError("Unable to create $resourceFile, $e")
                return
            }
        }
    }

    private fun log(msg: String) {
        if (processingEnv.options.containsKey("debug")) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "debug")
        }
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, msg)
    }

    private fun error(msg: String, element: Element, annotation: AnnotationMirror) {
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, msg, element, annotation)
    }

    private fun fatalError(msg: String) {
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "FATAL ERROR: $msg")
    }
}