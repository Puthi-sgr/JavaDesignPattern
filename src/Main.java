import jdk.jfr.TransitionFrom;

import javax.xml.crypto.dsig.Transform;


void main() {

    TransformDecorator loggingDecorator = new LoggingTransformDecorator(new JsonExportAxis());
    new StrategyFactory()
            .registerTransformAxis("xml", () -> new LoggingTransformDecorator(
                    new XmlExportAxis()
            ))
            .registerTransformAxis("json", () -> new TimeoutTransformDecorator(
                    new RetryTransformDecorator(
                            new LoggingTransformDecorator(
                                    new JsonExportAxis()
                            ),
                            3 // Number of retry attempts
                    ),
                    5 // Timeout in milliseconds
            ))
            .registerCompressionAxis("zip", () -> new LoggingCompressionDecorator(
                    new zipCompressionAxis()
            ))
            .registerCompressionAxis("gzip", gzipCompressionAxis::new)
            .registerFormatAxis("prettier", PrettierPrintFormat::new);

    ExporterClient client = new ExporterClient(
            StrategyFactory.getTransformAxis("xml"),
            StrategyFactory.getCompressionAxis("zip"),
            StrategyFactory.getFormatAxis("prettier")
    );

    String exported = client.export("   Sample Data to be Exported   ", 9);

    System.out.println("Final Exported Data: " + exported);
    System.out.println("Current Export Configuration: " + client.currentExportConfiguration());
}

//Each core algorithm (Strategy) will have its own decorator
//WE can decorate each strategy with additional functionalities

//Axes interface

//Transform axis, filter axis, Destination axis, Compression axis, Formatting axis
public interface Axis{
    String Apply(String input);
    String type();

    //Eg. transform
}

public interface TransformAxis extends Axis{
}
public interface FormatAxis extends Axis{
}
public interface CompressionAxis extends Axis{
}

//Strategy decorators
public abstract class TransformDecorator implements TransformAxis{
    protected final TransformAxis delegate;
    protected TransformDecorator(TransformAxis delegate){
        this.delegate = delegate; //Base logic
    }
}

public abstract class CompressionDecorator implements CompressionAxis{
    protected final CompressionAxis delegate;
    protected CompressionDecorator(CompressionAxis delegate){
        this.delegate = delegate; //Base logic
    }
}

//Concrete decorators for transform axis
public final class LoggingTransformDecorator extends TransformDecorator{
    public LoggingTransformDecorator(TransformAxis delegate){
        super(delegate);
    }

    @Override
    public String Apply(String input){
        long t0 = System.currentTimeMillis();
        try{
            return delegate.Apply(input);
        } finally {
            long t1 = System.currentTimeMillis();
            System.out.println("Transform took " + (t1 - t0) + " ms");
        }
    }

    @Override
    public String type() {
        return "Logging Decorator for " + delegate.type();
    }
}

public final class TimeoutTransformDecorator extends TransformDecorator{

    private final long timeoutMillis;

    public TimeoutTransformDecorator(TransformAxis delegate, long timeoutMillis){
        super(delegate);
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public String Apply(String input){
        long startTime = System.currentTimeMillis();
        String result = delegate.Apply(input);
        long endTime = System.currentTimeMillis();
        if (endTime - startTime > timeoutMillis) {
            throw new RuntimeException("Transformation exceeded timeout of " + timeoutMillis + " ms");
        }
        return result;
    }

    @Override
    public String type() {
        return "Timeout Decorator for " + delegate.type();
    }

}

public final class RetryTransformDecorator extends TransformDecorator{

    private final int attempts;
    public RetryTransformDecorator(TransformAxis delegate, int attempts){
        super(delegate);
        this.attempts = attempts;
    }

    @Override
    public String Apply(String input){
        for(int i = 0; i < attempts; i++) {
            try {
                return delegate.Apply(input);
            } catch (Exception e) {
                System.out.println("Attempt " + (i + 1) + " failed: " + e.getMessage());
                if (i == attempts - 1) {
                    throw e; //Rethrow after last attempt
                }
            } finally {
                System.out.println("Retry attempt " + (i + 1) + " completed.");
                System.out.println("Only " + (attempts - i - 1) + " attempts left.");
            }
        }
        return null; //Should never reach here
    }

    @Override
    public String type() {
        return "Retry Decorator for " + delegate.type();
    }
}

//Concrete decorators for compression axis
public final class LoggingCompressionDecorator extends CompressionDecorator{
    public LoggingCompressionDecorator(CompressionAxis delegate){
        super(delegate);
    }

    @Override
    public String Apply(String input){
        long t0 = System.currentTimeMillis();
        try{
            return delegate.Apply(input);
        } finally {
            long t1 = System.currentTimeMillis();
            System.out.println("Compression took " + (t1 - t0) + " ms");
        }
    }

    @Override
    public String type() {
        return "Logging Decorator for " + delegate.type();
    }
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


}

public static final class XmlExportAxis implements TransformAxis{

    @Override
    public String Apply(String sanitizedInput) {
        //Simple XML transformation to <>
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

        return format.Apply(
                        compression.Apply(
                                transform.Apply(data)));


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

