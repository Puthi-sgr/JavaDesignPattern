import jdk.jfr.TransitionFrom;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
void main() {
//We need an exporter
    //The consumer will have two options to consume two different types of exporters
    //1. We will need a contract interface that both exporters will implement
    //2. We will need an abstract class that will have custom hooks and packaged algorithm
    //3. The concrete class will extend the abstract but have to override the abstract methods
    //4. The consumer will use the abstract class reference to point to concrete class object
    new StrategyFactory()
            .registerTransformAxis("xml", XmlExportAxis::new)
            .registerTransformAxis("json", JsonExportAxis::new)
            .registerCompressionAxis("zip", zipCompressionAxis::new)
            .registerCompressionAxis("gzip", gzipCompressionAxis::new)
            .registerFormatAxis("prettier", PrettierPrintFormat::new);

    ExporterClient client = new ExporterClient(
            StrategyFactory.getTransformAxis("xml"),
            StrategyFactory.getCompressionAxis("gzip"),
            StrategyFactory.getFormatAxis("prettier")
    );

    String exported = client.export("   Sample Data to be Exported   ", 9);

    System.out.println("Final Exported Data: " + exported);
    System.out.println("Current Export Configuration: " + client.currentExportConfiguration());
}

//Create an axis to plug in new exporters (Strategy Pattern)
//Create a factory that creates a factory of axis (Factory of Factories Pattern)
//Create a registry to register new exporters dynamically (Registry Pattern)

//Axes interface

//Transform axis, filter axis, Destination axis, Compression axis, Formatting axis
public interface Axis{
    String Apply(String input);
    String type();

    //Eg. transform
}

public interface TransformAxis extends Axis{
    void Script();
}
public interface FormatAxis extends Axis{
    String identation(int level);
}
public interface CompressionAxis extends Axis{
    String levelOfCompression(int level);
}
public static final class StrategyFactory {

    private static final Map<String, Supplier<Axis>> registeredAxis = new HashMap<>();
    private static final Map<String, Supplier<TransformAxis>> registeredTransformAxis = new HashMap<>();
    private static final Map<String, Supplier<FormatAxis>> registeredFormatAxis = new HashMap<>();
    private static final Map<String, Supplier<CompressionAxis>> registeredCompressionAxis = new HashMap<>();



    public StrategyFactory registerAxis(String axisType, Supplier<Axis> axisSupplier) {
        registeredAxis.put(axisType.toLowerCase(), axisSupplier);
        return this;
    }

    public StrategyFactory registerTransformAxis(String axisType, Supplier<TransformAxis> axisSupplier) {
        registeredTransformAxis.put(axisType.toLowerCase(), axisSupplier);
        return this;
    }

    public StrategyFactory registerFormatAxis(String axisType, Supplier<FormatAxis> axisSupplier) {
        registeredFormatAxis.put(axisType.toLowerCase(), axisSupplier);
        return this;
    }

    public StrategyFactory registerCompressionAxis(String axisType, Supplier<CompressionAxis> axisSupplier) {
        registeredCompressionAxis.put(axisType.toLowerCase(), axisSupplier);
        return this;
    }

    public static Axis getAxis(String strategy) {
        Supplier<Axis> axisSupplier = registeredAxis.get(strategy.toLowerCase());
        if (axisSupplier != null) {
            return axisSupplier.get();
        }

        throw new IllegalArgumentException("No axis registered for strategy: " + strategy);
    }

    public static TransformAxis getTransformAxis(String strategy) {
        Supplier<TransformAxis> axisSupplier = registeredTransformAxis.get(strategy.toLowerCase());
        if (axisSupplier != null) {
            return axisSupplier.get();
        }

        throw new IllegalArgumentException("No transform axis registered for strategy: " + strategy);
    }

    public static CompressionAxis getCompressionAxis(String strategy) {
        Supplier<CompressionAxis> axisSupplier = registeredCompressionAxis.get(strategy.toLowerCase());
        if (axisSupplier != null) {
            return axisSupplier.get();
        }

        throw new IllegalArgumentException("No compression axis registered for strategy: " + strategy);
    }

    public static FormatAxis getFormatAxis(String strategy) {
        Supplier<FormatAxis> axisSupplier = registeredFormatAxis.get(strategy.toLowerCase());
        if (axisSupplier != null) {
            return axisSupplier.get();
        }

        throw new IllegalArgumentException("No format axis registered for strategy: " + strategy);
    }

}
public interface Exporter{
    String export(String input);
    String type();
}

//Concrete axis
//Compression strategy axis
public static final class zipCompressionAxis implements CompressionAxis{
    @Override
    public String Apply(String input) {
        return "Zipped(" + input + ")";
    }

    @Override
    public String type() {
        return "Zip Compression Axis";
    }

    @Override
    public String levelOfCompression(int l) {
        return "Level" + l + "Compression";
    }
}

public static final class gzipCompressionAxis implements CompressionAxis{
    @Override
    public  String Apply(String input) {
        return "GZipped(" + input + ")";
    }

    @Override
    public String type() {
        return "GZip Compression Axis";
    }

    @Override
    public String levelOfCompression(int l) {
        return "**********************" + l + "*********************";
    }
}

//Transform strategy axis
//Json, Xml, Csv, Yml
public static final class JsonExportAxis implements TransformAxis{
    @Override
    public String Apply(String sanitizedInput) {
        //Simple JSON transformation to {}
        return "{\"data in JSON\": \"" + sanitizedInput + "\"}";
    };

    @Override
    public String type() {
        return "JSON Exporter";
    }

    @Override
    public void Script(){
        System.out.println("Executing JSON Transformation Script...");
    }
}

public static final class XmlExportAxis implements TransformAxis{
    @Override
    public void Script() {
        System.out.println("Executing XML Transformation Script...");
    }

    @Override
    public String Apply(String sanitizedInput) {
        //Simple XML transformation to <>
        Script();
        beforeTransform(sanitizedInput);
        return "<data>" + afterTransform(sanitizedInput) + "</data>";
    };

    @Override
    public String type() {
        return "XML Exporter";
    }

    private void beforeTransform(String sanitizedInput) {
        System.out.println("Preparing to transform to XML....................: " + sanitizedInput);
    }

    private String afterTransform(String transformedOutput){
        //Adding XML declaration
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + transformedOutput + "\n<!-- Exported by XmlExport -->";
    }
}

public static final class PrettierPrintFormat implements FormatAxis{
    @Override
    public String Apply(String input) {

        return "PrettierFormatted(" + input + ")";
    }

    @Override
    public String type() {
        return "Prettier Print Format Axis";
    }

    @Override
    public String identation(int level) {
        if (level <= 0) return "";
        return "---".repeat(level); // 2 spaces per level
    }
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



public final class ExporterClient{ //The context for the strategy
    private final CompressionAxis compression;
    private final FormatAxis format;
    private final TransformAxis transform;

    public ExporterClient(TransformAxis transform, CompressionAxis compression, FormatAxis format){

        this.transform = transform;
        this.compression = compression;
        this.format = format;
    }

    public  String export(String data, int identLevel){

        String identation = format.identation(identLevel);
        String res = format.Apply(
                        compression.Apply(
                                transform.Apply(data)));

        return identation + res + identation;
    }


    public String currentExportConfiguration(){
        Map<Integer, String> config = new HashMap<>();
        config.put(1, transform.type());
        config.put(2, compression.type());
        config.put(3, format.type());
        return config.toString();

    }
}

public enum ExporterType{
    JSON,
    XML,
    CSV
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

