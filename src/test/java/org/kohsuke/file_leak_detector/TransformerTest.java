package org.kohsuke.file_leak_detector;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.kohsuke.file_leak_detector.transform.ClassTransformSpec;
import org.kohsuke.file_leak_detector.transform.TransformerImpl;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * @author Kohsuke Kawaguchi
 */
@RunWith(Parameterized.class)
public class TransformerTest {
    private static final List<ClassTransformSpec> specs = AgentMain.createSpec();

    private final Class<?> c;

    public TransformerTest(Class<?> c) {
        this.c = c;
    }

    @Test
    public void testInstrumentations() throws Exception {
        TransformerImpl t = new TransformerImpl(specs);

        String name = c.getName().replace('.', '/');
        byte[] data;
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull("Could not load " + name + ".class", resource);
            data = resource.readAllBytes();
        }
        byte[] data2 = t.transform(name, data);

//        File classFile = new File("/tmp/" + name + ".class");
//        classFile.getParentFile().mkdirs();
//        FileOutputStream o = new FileOutputStream(classFile);
//        o.write(data2);
//        o.close();

        final String errors;
        ClassReader classReader = new ClassReader(data2);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
                PrintWriter pw = new PrintWriter(osw)) {
            CheckClassAdapter.verify(classReader, false, pw);
            errors = baos.toString(StandardCharsets.UTF_8);
        }
        assertTrue("Verification failed for " + c + "\n" + errors, errors.isEmpty());
    }

    @Parameters(name = "{index} - {0}")
    public static List<Object[]> specs() throws Exception {
        List<Object[]> r = new ArrayList<>();
        for (ClassTransformSpec s : specs) {
            Class<?> c = TransformerTest.class.getClassLoader().loadClass(s.name.replace('/', '.'));
            r.add(new Object[] {c});
        }
        return r;
    }
}
