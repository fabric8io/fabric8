package biz.c24.io.training.statements;


/**
 * The Statements data model.
 * 
 * @author C24 Integration Objects;
 **/
public class StatementsDataModel extends biz.c24.io.api.data.DataModel 
{
    private static StatementsDataModel instance;
    private static boolean initialized;

    protected StatementsDataModel(boolean init)
    {
        if (init)
            init();
    }

    /**
     * Gets the singleton instance.
     * @return The instance.
     **/
    public static biz.c24.io.training.statements.StatementsDataModel getInstance()
    {
        if (!initialized)
        {
            synchronized (biz.c24.io.training.statements.StatementsDataModel.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.StatementsDataModel(false);
                    instance.init();
                    initialized = true;
                }
            }
        }
        return instance;
    }

    private void init()
    {
        setName("Statements");
        setTargetNamespace("http://www.c24.biz/io/Training/Statements");
        setElementFormDefault(biz.c24.io.api.data.FormEnum.QUALIFIED);
        setAttributeFormDefault(biz.c24.io.api.data.FormEnum.UNQUALIFIED);
        setIdGeneratorMethodDefault(biz.c24.io.api.data.IDGeneratorMethodEnum.NATIVE);
        setSource(new biz.c24.io.api.presentation.XMLSource());
        setSink(new biz.c24.io.api.presentation.XMLSink());
    }

}
