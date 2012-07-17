package biz.c24.io.training.statements;


/**
 * The StatementLine complex data type.
 * 
 * @author C24 Integration Objects;
 * @see biz.c24.io.training.statements.StatementLine
 **/
public class StatementLineClass extends biz.c24.io.api.data.ComplexDataType 
{
    protected static StatementLineClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected StatementLineClass()
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
            synchronized (biz.c24.io.training.statements.StatementLineClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.StatementLineClass();
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
        setName("StatementLine");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
        setValidObjectClass(biz.c24.io.training.statements.StatementLine.class);
        setContentModel(biz.c24.io.api.data.ContentModelEnum.SEQUENCE);
        biz.c24.io.api.data.Element element = null;
        addElementDecl(element = new biz.c24.io.api.data.Element("PostingDate", 1, 1, biz.c24.io.training.statements.ISODateClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("ValueDate", 1, 1, biz.c24.io.training.statements.ISODateClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("DrCr", 1, 1, biz.c24.io.training.statements.DebitCreditIndicatorClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("TxAmount", 1, 1, biz.c24.io.training.statements.CurrencyAndAmountClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("PostingNarrative", 1, 1, biz.c24.io.training.statements.Max140TextClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
