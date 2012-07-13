package biz.c24.io.training.statements;

import java.util.Arrays;

/**
 * Document Root. <p/>
 * This object is composed of the following <i>elements</i>:
 * <ul>
 * <li><b>StatementFile</b> of type {@link biz.c24.io.training.statements.StatementFile} (1)</li>
 * <li><b>Statement</b> of type {@link biz.c24.io.training.statements.Statement} (1)</li>
 * </ul>
 * @author C24 Integration Objects;
 * @see biz.c24.io.training.statements.StatementsDocumentRootElement.StatementsDocumentRootClass
 **/
public class StatementsDocumentRoot extends biz.c24.io.api.data.DocumentRoot 
{
    private static final java.lang.String[] NATIVE_ELEMENTS = new String[] {"Statement", "StatementFile"};
    private biz.c24.io.training.statements.StatementFile statementFile;
    private biz.c24.io.training.statements.Statement statement;

    /**
     * Constructs a new instance defined by the default element.
     **/
    public StatementsDocumentRoot()
    {
        this(biz.c24.io.training.statements.StatementsDocumentRootElement.StatementsDocumentRootClass.getInstance().getNullDefiningElementDecl());
    }

    /**
     * Constructs a new instance defined by the specified element.
     * @param definingElementDecl The element which defines the object.
     **/
    public StatementsDocumentRoot(biz.c24.io.api.data.Element definingElementDecl)
    {
        super(definingElementDecl);
    }

    /**
     * Constructs a new instance cloned from the specified object.
     * @param clone The object to be cloned.
     **/
    public StatementsDocumentRoot(biz.c24.io.training.statements.StatementsDocumentRoot clone)
    {
        super(clone);
    }

    /**
     * Adds <code>value</code> as an element called <code>name</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getElement}.
     **/
    public void addElement(java.lang.String name, java.lang.Object value)
    {
        name = makeSubstitution(name, -1);
        switch (Arrays.binarySearch(NATIVE_ELEMENTS, name))
        {
            case 1:
                setStatementFile((biz.c24.io.training.statements.StatementFile) value);
                return;
            case 0:
                setStatement((biz.c24.io.training.statements.Statement) value);
                return;
            default:
                super.addElement(name, value);
        }
    }

    /**
     * Creates and returns a shallow clone of this object.
     * @see #cloneDeep()
     **/
    public java.lang.Object clone()
    {
        return new biz.c24.io.training.statements.StatementsDocumentRoot(this);
    }

    /**
     * Creates and returns a deep clone of this object.
     * @return The new object.
     * @see #clone()
     **/
    public biz.c24.io.api.data.ComplexDataObject cloneDeep() throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.api.data.ComplexDataObject obj = new biz.c24.io.training.statements.StatementsDocumentRoot(this);
        cloneDeep(obj);
        return obj;
    }

    protected void cloneDeep(biz.c24.io.api.data.ComplexDataObject clone) throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.training.statements.StatementsDocumentRoot obj = (biz.c24.io.training.statements.StatementsDocumentRoot) clone;
        obj.statementFile = (biz.c24.io.training.statements.StatementFile) biz.c24.io.api.Utils.cloneDeep(this.statementFile, obj, "StatementFile");
        obj.statement = (biz.c24.io.training.statements.Statement) biz.c24.io.api.Utils.cloneDeep(this.statement, obj, "Statement");
    }

    /**
     * Creates, adds and returns a new Statement (1).
     * @return The new value.
     **/
    public biz.c24.io.training.statements.Statement createStatement()
    {
        biz.c24.io.training.statements.Statement obj = (biz.c24.io.training.statements.Statement) getElementDecl("Statement").createObject();
        setStatement(obj);
        return obj;
    }

    /**
     * Creates, adds and returns a new StatementFile (1).
     * @return The new value.
     **/
    public biz.c24.io.training.statements.StatementFile createStatementFile()
    {
        biz.c24.io.training.statements.StatementFile obj = (biz.c24.io.training.statements.StatementFile) getElementDecl("StatementFile").createObject();
        setStatementFile(obj);
        return obj;
    }

    public boolean equals(java.lang.Object obj)
    {
        if(obj instanceof biz.c24.io.api.data.ComplexDataObject)
            return equalContents((biz.c24.io.api.data.ComplexDataObject) obj, true, true, true, true);
        else
            return obj.equals(this);
    }

    /**
     * Returns the element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are: <b>statementFile, statement</b>.
     **/
    public java.lang.Object getElement(java.lang.String name, int index)
    {
        int i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        if (i < 0)
        {
            name = getSubstitute(name);
            i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        }
        switch (i)
        {
            case 1:
                return this.statementFile;
            case 0:
                return this.statement;
            default:
                return super.getElement(name, index);
        }
    }

    /**
     * Returns the count of elements called <code>name</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getElement}.
     **/
    public int getElementCount(java.lang.String name)
    {
        if (null == name) throw new NullPointerException(toString() + " was asked to calculate elements without name");
        int i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        if (i < 0)
        {
            name = getSubstitute(name);
            i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        }
        switch (i)
        {
            case 1:
                return this.statementFile == null ? 0 : 1;
            case 0:
                return this.statement == null ? 0 : 1;
            default:
                return super.getElementCount(name);
        }
    }

    /**
     * Returns the element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getElement}.
     **/
    public int getElementIndex(java.lang.String name, java.lang.Object element)
    {
        int i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        if (i < 0)
        {
            name = getSubstitute(name);
            i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        }
        switch (i)
        {
            case 1:
                return this.statementFile != null && this.statementFile.equals(element) ? 0 : -1;
            case 0:
                return this.statement != null && this.statement.equals(element) ? 0 : -1;
            default:
                return super.getElementIndex(name, element);
        }
    }

    /**
     * Gets the value of Statement (1).
     * @return The value.
     **/
    public biz.c24.io.training.statements.Statement getStatement()
    {
        return this.statement;
    }

    /**
     * Gets the value of StatementFile (1).
     * @return The value.
     **/
    public biz.c24.io.training.statements.StatementFile getStatementFile()
    {
        return this.statementFile;
    }

    public int getTotalElementCount()
    {
        int count = 0;
        count += this.statementFile == null ? 0 : 1;
        count += this.statement == null ? 0 : 1;
        return count;
    }

    public int hashCode()
    {
        return this.toString().length();
    }

    /**
     * Removes the element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getElement}.
     **/
    public void removeElement(java.lang.String name, int index)
    {
        name = unmakeSubstitution(name, index);
        switch (Arrays.binarySearch(NATIVE_ELEMENTS, name))
        {
            case 1:
                setStatementFile(null);
                return;
            case 0:
                setStatement(null);
                return;
            default:
            super.removeElement(name, index);
        }
    }

    /**
     * Sets <code>value</code> as an element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getElement}.
     **/
    public void setElement(java.lang.String name, int index, java.lang.Object value)
    {
        name = makeSubstitution(name, index);
        switch (Arrays.binarySearch(NATIVE_ELEMENTS, name))
        {
            case 1:
                setStatementFile((biz.c24.io.training.statements.StatementFile) value);
                return;
            case 0:
                setStatement((biz.c24.io.training.statements.Statement) value);
                return;
            default:
                super.setElement(name, index, value);
        }
    }

    /**
     * Sets the value of Statement (1).
     * @param value The new value.
     **/
    public void setStatement(biz.c24.io.training.statements.Statement value)
    {
        this.statement = value;
        if (this.statement != null)
            ((biz.c24.io.api.data.ComplexDataObject) this.statement).setParent(this, "Statement");
    }

    /**
     * Sets the value of StatementFile (1).
     * @param value The new value.
     **/
    public void setStatementFile(biz.c24.io.training.statements.StatementFile value)
    {
        this.statementFile = value;
        if (this.statementFile != null)
            ((biz.c24.io.api.data.ComplexDataObject) this.statementFile).setParent(this, "StatementFile");
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException
    {
        out.writeObject(this.statementFile);
        out.writeObject(this.statement);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException
    {
        this.statementFile = (biz.c24.io.training.statements.StatementFile) in.readObject();
        this.statement = (biz.c24.io.training.statements.Statement) in.readObject();
    }

}
