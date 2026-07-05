package smth.template_mod;

import n.paradox.aslib.initializer.generator.GenerateInitializer;
import n.paradox.aslib.initializer.generator.InitializerClass;
import n.paradox.aslib.initializer.generator.LoaderType;

//its main class*
@GenerateInitializer(loader = LoaderType.Both, modId = "template")
public class TemplateMod implements InitializerClass {
    @Override
    public void init() {
        System.out.println("HALLO :D");
    }
}
