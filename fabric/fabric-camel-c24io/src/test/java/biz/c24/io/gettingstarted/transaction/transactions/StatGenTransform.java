package biz.c24.io.gettingstarted.transaction.transactions;


/**
 * 
 * @author C24 Integration Objects;
 **/
public class StatGenTransform extends biz.c24.io.api.transform.Transform 
{
    public final biz.c24.io.gettingstarted.transaction.transactions.StatGenTransform.JustScroogeFilter JUSTSCROOGE = new biz.c24.io.gettingstarted.transaction.transactions.StatGenTransform.JustScroogeFilter();
    public final biz.c24.io.gettingstarted.transaction.transactions.StatGenTransform.RecordToStmtLineTransform RECORDTOSTMTLINE = new biz.c24.io.gettingstarted.transaction.transactions.StatGenTransform.RecordToStmtLineTransform();

    {
        register(JUSTSCROOGE);
        register(RECORDTOSTMTLINE);
    }

    public StatGenTransform()
    {
        super(new biz.c24.io.api.data.DataType[] {biz.c24.io.gettingstarted.transaction.TransactionsClass.getInstance()}, new biz.c24.io.api.data.DataType[] {biz.c24.io.training.statements.StatementFileClass.getInstance()});
    }

    public StatGenTransform(biz.c24.io.api.data.Element[] inputElements, biz.c24.io.api.data.Element[] outputElements)
    {
        super(new biz.c24.io.api.data.DataType[] {biz.c24.io.gettingstarted.transaction.TransactionsClass.getInstance()}, new biz.c24.io.api.data.DataType[] {biz.c24.io.training.statements.StatementFileClass.getInstance()}, inputElements, outputElements);
    }

    protected StatGenTransform(biz.c24.io.api.data.DataType[] inputTypes, biz.c24.io.api.data.DataType[] outputTypes)
    {
        super(inputTypes, outputTypes);
    }

    protected StatGenTransform(biz.c24.io.api.data.DataType[] inputTypes, biz.c24.io.api.data.DataType[] outputTypes, biz.c24.io.api.data.Element[] inputElements, biz.c24.io.api.data.Element[] outputElements)
    {
        super(inputTypes, outputTypes, inputElements, outputElements);
    }

    public java.lang.String getName()
    {
        return "StatGen.tfd";
    }

    public static void main(java.lang.String[] args)
    {
        if (args.length == 2)
        {
            org.apache.log4j.Appender appender = new org.apache.log4j.ConsoleAppender(new org.apache.log4j.SimpleLayout());
            
            java.util.Set loggers = new java.util.HashSet();
            
            try
            {
                biz.c24.io.api.transform.Transform transform = new StatGenTransform();
                loggers.add(transform.getLog());
                
                biz.c24.io.api.data.Element inputElement0 = null;
                transform.setInput(0, inputElement0 = transform.getPossibleInputElementDecls(0).length > 0 ? (biz.c24.io.api.data.Element) java.lang.Class.forName(transform.getPossibleInputElementDecls(0)[0]).getMethod("getInstance", null).invoke(null, null) : transform.getInput(0));
                loggers.add(inputElement0.getLog());
                
                biz.c24.io.api.data.Element outputElement0 = null;
                transform.setOutput(0, outputElement0 = transform.getPossibleOutputElementDecls(0).length > 0 ? (biz.c24.io.api.data.Element) java.lang.Class.forName(transform.getPossibleOutputElementDecls(0)[0]).getMethod("getInstance", null).invoke(null, null) : transform.getOutput(0));
                loggers.add(outputElement0.getLog());
                
                for (java.util.Iterator it = loggers.iterator(); it.hasNext(); )
                {
                    org.apache.log4j.Logger logger = (org.apache.log4j.Logger) it.next();
                    logger.addAppender(appender);
                    logger.setLevel(org.apache.log4j.Level.OFF); // change this to see console logging
                }
                
                java.lang.System.out.println("Running...");
                java.util.List inputBeanList = new java.util.LinkedList();
                java.lang.Object[][] inputBeanArray = new java.lang.Object[1][];
                biz.c24.io.api.presentation.Source source0 = transform.getInput(0).getModel().source();
                for (int i=0; ; i++)
                {
                    java.lang.String inputFileName = args[0];
                    java.io.File inputFile = new java.io.File(i == 0 ? inputFileName : (inputFileName.indexOf('.') == -1 ? inputFileName + i : inputFileName.substring(0, inputFileName.lastIndexOf(".")) + i + inputFileName.substring(inputFileName.lastIndexOf("."))));
                    if (!inputFile.canRead())
                        break;
                    java.lang.System.out.println("Reading '"+inputFile.getAbsolutePath()+"' ...");
                    source0.setInputStream(new java.io.FileInputStream(inputFile));
                    inputBeanList.add(source0.readObject(inputElement0));
                    source0.getInputStream().close();
                }
                inputBeanArray[0] = inputBeanList.toArray(new java.lang.Object[inputBeanList.size()]);
                    inputBeanList.clear();
                
                java.lang.Object[][] outputBeanArray = transform.transform(inputBeanArray);
                biz.c24.io.api.presentation.Sink sink0 = transform.getOutput(0).getModel().sink();
                for (int i=0; i<outputBeanArray[0].length; i++)
                {
                    java.lang.String outputFileName = args[1];
                    java.io.File outputFile = new java.io.File(i == 0 ? outputFileName : (outputFileName.indexOf('.') == -1 ? outputFileName + i : outputFileName.substring(0, outputFileName.lastIndexOf(".")) + i + outputFileName.substring(outputFileName.lastIndexOf("."))));
                    java.lang.System.out.println("Writing '"+outputFile.getAbsolutePath()+"' ...");
                    sink0.setOutputStream(new java.io.FileOutputStream(outputFile));
                    sink0.writeObject((biz.c24.io.api.data.ComplexDataObject) outputBeanArray[0][i]);
                    sink0.getOutputStream().close();
                }
                
                java.lang.System.out.println("Finished");
            }
            catch (java.lang.Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                for (java.util.Iterator it = loggers.iterator(); it.hasNext(); )
                    ((org.apache.log4j.Logger) it.next()).removeAppender(appender);
            }
        }
        else
            java.lang.System.out.println("Usage: java StatGenTransform <input 'Transactions' filename> <output 'StatementFile' filename>");
    }

    public java.lang.Object[][] transform(final java.lang.Object[][] inArr) throws biz.c24.io.api.data.ValidationException
    {
        checkRequiredInputs(inArr);
        java.lang.Object[][] outArr = transform(inArr, init(inArr));
        checkRequiredOutputs(outArr);
        return outArr;
    }

    protected java.lang.Object[][] transform(java.lang.Object[][] inArr, java.lang.Object[][] outArr) throws biz.c24.io.api.data.ValidationException
    {
        initElement(outArr[0], "Statement", "StmtLine", transform(RECORDTOSTMTLINE, new java.lang.Object[] {filter(JUSTSCROOGE, new java.lang.Object[] {}, resolve(inArr[0], "CustomerDetails", false, true))}, 0, null), false, true);
        return outArr;
    }

    public biz.c24.io.training.statements.StatementFile transform(final biz.c24.io.gettingstarted.transaction.Transactions input1) throws biz.c24.io.api.data.ValidationException
    {
        java.lang.Object[][] outArr = transform(new java.lang.Object[][]{{input1}});
        return (biz.c24.io.training.statements.StatementFile) (outArr.length > 0 && outArr[0].length > 0 ? outArr[0][0] : null);
    }

    /**
     * 
     * @author C24 Integration Objects;
     **/
    public class JustScroogeFilter extends biz.c24.io.api.transform.Filter 
    {

        public java.lang.Object filter(final java.lang.Object arg, final java.lang.Object[] inArr) throws biz.c24.io.api.data.ValidationException
        {
            java.lang.Object obj = EQUALS.f(resolve(arg, "Name", false, true), "Mr Scrooge");
            if (booleanOrValue(obj))
                return resolve(arg, "", false, true);
            else
                return null;
        }

    }


    /**
     * 
     * @author C24 Integration Objects;
     **/
    public class RecordToStmtLineTransform extends biz.c24.io.api.transform.Transform 
    {

        public RecordToStmtLineTransform()
        {
            super(new biz.c24.io.api.data.DataType[] {biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.getInstance()}, new biz.c24.io.api.data.DataType[] {biz.c24.io.training.statements.StatementLineClass.getInstance()});
        }

        public RecordToStmtLineTransform(biz.c24.io.api.data.Element[] inputElements, biz.c24.io.api.data.Element[] outputElements)
        {
            super(new biz.c24.io.api.data.DataType[] {biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.getInstance()}, new biz.c24.io.api.data.DataType[] {biz.c24.io.training.statements.StatementLineClass.getInstance()}, inputElements, outputElements);
        }

        protected RecordToStmtLineTransform(biz.c24.io.api.data.DataType[] inputTypes, biz.c24.io.api.data.DataType[] outputTypes)
        {
            super(inputTypes, outputTypes);
        }

        protected RecordToStmtLineTransform(biz.c24.io.api.data.DataType[] inputTypes, biz.c24.io.api.data.DataType[] outputTypes, biz.c24.io.api.data.Element[] inputElements, biz.c24.io.api.data.Element[] outputElements)
        {
            super(inputTypes, outputTypes, inputElements, outputElements);
        }

        public java.lang.String getName()
        {
            return "Record to StmtLine";
        }

        public java.lang.Object[][] transform(final java.lang.Object[][] inArr) throws biz.c24.io.api.data.ValidationException
        {
            checkRequiredInputs(inArr);
            java.lang.Object[][] outArr = transform(inArr, init(inArr));
            checkRequiredOutputs(outArr);
            return outArr;
        }

        protected java.lang.Object[][] transform(java.lang.Object[][] inArr, java.lang.Object[][] outArr) throws biz.c24.io.api.data.ValidationException
        {
            initElement(outArr[0], null, "TxAmount", CAST.f(resolve(inArr[0], "Amount", false, true), biz.c24.io.api.data.FloatDataType.getInstance()), false, true);
            initElement(outArr[0], null, "PostingNarrative", resolve(inArr[0], "Name", false, true), false, true);
            return outArr;
        }

    }

}
