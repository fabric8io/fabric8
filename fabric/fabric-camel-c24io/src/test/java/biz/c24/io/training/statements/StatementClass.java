package biz.c24.io.training.statements;


/**
 * The Statement complex data type.
 * 
 * @author C24 Integration Objects;
 * @see biz.c24.io.training.statements.Statement
 **/
public class StatementClass extends biz.c24.io.api.data.ComplexDataType 
{
    protected static StatementClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected StatementClass()
    {
    }

    /**
     * Gets the singleton instance of this type.
     * @return The type, or its supertype if present.
     **/
    public static biz.c24.io.api.data.DataType getInstance()
    {
        if (!initialized)
        {
            synchronized (biz.c24.io.training.statements.StatementClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.StatementClass();
                    instance.init();
                    initialized = true;
                }
            }
        }
        return instance;
    }

    /**
     * Called internally to initialize this type.
     **/
    protected void init()
    {
        setName("Statement");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
        setValidObjectClass(biz.c24.io.training.statements.Statement.class);
        setContentModel(biz.c24.io.api.data.ContentModelEnum.SEQUENCE);
        biz.c24.io.api.data.Element element = null;
        addElementDecl(element = new biz.c24.io.api.data.Element("Hdr", 1, 1, biz.c24.io.training.statements.HeaderClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("StmtLine", 0, biz.c24.io.api.data.DataComponent.CARDINALITY_UNBOUNDED, biz.c24.io.training.statements.StatementLineClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("Tlr", 1, 1, biz.c24.io.training.statements.TrailerClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
