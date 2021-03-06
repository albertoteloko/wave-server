package com.acs.wave.converter.template;

import com.acs.wave.router.functional.BodyWriter;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;


public class ThymeleafEngine implements BodyWriter<TemplateModel> {

    public final TemplateEngine templateEngine;

    ThymeleafEngine(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public byte[] write(TemplateModel body) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter printStream = new PrintWriter(out)) {
            templateEngine.process(body.templateName, toContext(body), printStream);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Context toContext(TemplateModel body) {
        Context context = new Context();
        body.forEach(context::setVariable);
        return context;
    }
}
