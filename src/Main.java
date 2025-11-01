//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
void main() {
//We need an exporter
    //The consumer will have two options to consume two different types of exporters
    //1. We will need a contract interface that both exporters will implement
    //2. We will need an abstract class that will have custom hooks and packaged algorithm
    //3. The concrete class will extend the abstract but have to override the abstract methods
    //4. The consumer will use the abstract class reference to point to concrete class object
    new ExporterRegistry().register("json", JsonExport::new).register("xml", XmlExport::new).register("csv", CsvExport::new);


    String dataSample = "   Sample Data for Export   ";

    String res =    ExporterRegistry.getExporter("xml").export(dataSample);



    System.out.println("Exported using " + ":\n" + res + "\n");

}


public interface Exporter{
    String export(String input);
    String type();
}

public abstract class AbstractExporter implements Exporter{
    @Override
    public final String export(String input){
        validate(input);
        String sanitizedInput = sanitize(input);
        beforeTransform(input, sanitizedInput);
        String transformed = transform(sanitizedInput);
        return afterTransform(transformed);
    }

    //Concrete method no overriding allowed, but can be actually overridden
    protected final void validate(String input){
        if(input == null || input.isEmpty()){
            throw new IllegalArgumentException("Input cannot be null or empty");
        }
    }

    //Customer hook methods
    protected String sanitize(String input){
       //remove leading/trailing spaces
         return input.trim();
    }
    protected void beforeTransform(String originalInput, String sanitizedInput) {
        //hook method - default implementation does nothing
    }
    protected String afterTransform(String transformedOutput) {
        //hook method - default implementation returns the output as is
        return transformedOutput;
    }

    protected abstract String transform(String sanitizedInput);
}

public final class JsonExport extends AbstractExporter{
    @Override
    protected String transform(String sanitizedInput) {
        //Simple JSON transformation to {}
        return "{\"data in JSON\": \"" + sanitizedInput + "\"}";
    };

    @Override
    public String type() {
        return "JSON Exporter";
    }
}

public final class XmlExport extends AbstractExporter{
    @Override
    protected String transform(String sanitizedInput) {
        //Simple XML transformation to <>
        return "<data>" + sanitizedInput + "</data>";
    };

    @Override
    public String type() {
        return "XML Exporter";
    }

    @Override
    protected void beforeTransform(String originalInput, String sanitizedInput) {
        System.out.println("Preparing to transform to XML....................: " + sanitizedInput);
    }

    @Override
    protected String afterTransform(String transformedOutput){
        //Adding XML declaration
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + transformedOutput + "\n<!-- Exported by XmlExport -->";
    }
}

public final class CsvExport extends AbstractExporter {
    @Override
    protected String transform(String sanitizedInput) {
        //Advance string csv transformation
        return "data_in_CSV," + sanitizedInput.replace(" ", "_");
    }

    @Override
    public String type() {
        return "CSV Exporter";
    }
}

public final class ExporterClient{
    public String exportAs(ExporterType type, String data){
        ExporterFactory exporterFactory = new ExporterFactory();
        return exporterFactory.create(type).export(data);
    }
}

public enum ExporterType{
    JSON,
    XML,
    CSV
}

public final class ExporterFactory{
    public Exporter create(ExporterType type){
        return switch(type){
            case JSON -> new JsonExport();
            case XML -> new XmlExport();
            case CSV -> new CsvExport();
        };
    }
}

public final class ExporterRegistry{
    //This class can be used to register and retrieve exporters dynamically
    public static final Map<String, Supplier<Exporter>> listedExporters = new HashMap<>();

    public ExporterRegistry register(String type, Supplier<Exporter> exporterSupplier){
        listedExporters.put(type.toLowerCase(), exporterSupplier);
        return this;
    }

    public static Exporter getExporter(String type){
        Supplier<Exporter>  exporterSupplier = listedExporters.get(type.toLowerCase()); //Gets the exporter supplier of that type
        if(exporterSupplier != null) {
            return exporterSupplier.get(); //Returns the exporter instance
        }
        throw new IllegalArgumentException("No exporter registered for type: " + type);
    }
}