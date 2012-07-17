package biz.c24.io.training.statements;


/**
 * The StatementFile complex data type.
 * 
 * @author C24 Integration Objects;
 * @see biz.c24.io.training.statements.StatementFile
 **/
public class StatementFileClass extends biz.c24.io.api.data.ComplexDataType 
{
    protected static StatementFileClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected StatementFileClass()
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
            synchronized (biz.c24.io.training.statements.StatementFileClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.StatementFileClass();
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
        setName("StatementFile");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
        setValidObjectClass(biz.c24.io.training.statements.StatementFile.class);
        setContentModel(biz.c24.io.api.data.ContentModelEnum.SEQUENCE);
        biz.c24.io.api.data.Element element = null;
        addElementDecl(element = new biz.c24.io.api.data.Element("Statement", 0, biz.c24.io.api.data.DataComponent.CARDINALITY_UNBOUNDED, biz.c24.io.training.statements.StatementElement.getInstance(), null));
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
