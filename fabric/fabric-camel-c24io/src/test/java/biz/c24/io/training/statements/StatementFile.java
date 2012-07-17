package biz.c24.io.training.statements;

import java.util.Arrays;

/**
 * StatementFile. <p/>
 * This object is composed of the following <i>element</i>:
 * <ul>
 * <li><b>Statement</b> of type {@link biz.c24.io.training.statements.Statement} (0..*)</li>
 * </ul>
 * @author C24 Integration Objects;
 * @see biz.c24.io.training.statements.StatementFileClass
 **/
public class StatementFile extends biz.c24.io.api.data.ComplexDataObject 
{
    private static final java.lang.String[] NATIVE_ELEMENTS = new String[] {"Statement"};
    private biz.c24.io.training.statements.Statement[] statement;

    /**
     * Constructs a new instance defined by the default element.
     **/
    public StatementFile()
    {
        this(biz.c24.io.training.statements.StatementFileClass.getInstance().getNullDefiningElementDecl());
    }

    /**
     * Constructs a new instance defined by the specified element.
     * @param definingElementDecl The element which defines the object.
     **/
    public StatementFile(biz.c24.io.api.data.Element definingElementDecl)
    {
        super(definingElementDecl);
    }

    /**
     * Constructs a new instance defined by the specified element and type.
     * @param definingElementDecl The element which defines the object.
     * @param type The type which defines the object.
     **/
    public StatementFile(biz.c24.io.api.data.Element definingElementDecl, biz.c24.io.api.data.ComplexDataType type)
    {
        super(definingElementDecl, type);
    }

    /**
     * Constructs a new instance cloned from the specified object.
     * @param clone The object to be cloned.
     **/
    public StatementFile(biz.c24.io.training.statements.StatementFile clone)
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
            case 0:
                addStatement((biz.c24.io.training.statements.Statement) value);
                return;
            default:
                super.addElement(name, value);
        }
    }

    /**
     * Adds a Statement (0..*).
     * @param value The new Statement.
     **/
    public void addStatement(biz.c24.io.training.statements.Statement value)
    {
        biz.c24.io.training.statements.Statement[] temp = this.statement;
        this.statement = new biz.c24.io.training.statements.Statement[temp == null ? 1 : (temp.length+1)];
        if (temp != null)
            java.lang.System.arraycopy(temp, 0, this.statement, 0, temp.length);
        this.statement[this.statement.length-1] = value;
        ((biz.c24.io.api.data.ComplexDataObject) value).setParent(this, "Statement");
    }

    /**
     * Creates and returns a shallow clone of this object.
     * @see #cloneDeep()
     **/
    public java.lang.Object clone()
    {
        return new biz.c24.io.training.statements.StatementFile(this);
    }

    /**
     * Creates and returns a deep clone of this object.
     * @return The new object.
     * @see #clone()
     **/
    public biz.c24.io.api.data.ComplexDataObject cloneDeep() throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.api.data.ComplexDataObject obj = new biz.c24.io.training.statements.StatementFile(this);
        cloneDeep(obj);
        return obj;
    }

    protected void cloneDeep(biz.c24.io.api.data.ComplexDataObject clone) throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.training.statements.StatementFile obj = (biz.c24.io.training.statements.StatementFile) clone;
        obj.statement = (biz.c24.io.training.statements.Statement[]) biz.c24.io.api.Utils.cloneDeep(this.statement, obj, "Statement");
    }

    /**
     * Creates, adds and returns a new Statement (0..*).
     * @return The new value.
     **/
    public biz.c24.io.training.statements.Statement createStatement()
    {
        biz.c24.io.training.statements.Statement obj = (biz.c24.io.training.statements.Statement) getElementDecl("Statement").createObject();
        addStatement(obj);
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
     * The legal value(s) for <code>name</code> are: <b>statement</b>.
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
            case 0:
            if (this.statement == null)
                throw new java.lang.ArrayIndexOutOfBoundsException();
            else
                return this.statement[index];
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
            case 0:
                return this.statement == null ? 0 : this.statement.length;
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
            case 0:
                return getStatementIndex((biz.c24.io.training.statements.Statement) element);
            default:
                return super.getElementIndex(name, element);
        }
    }

    /**
     * Gets the value of Statement (0..*).
     * @return The value.
     **/
    public biz.c24.io.training.statements.Statement[] getStatement()
    {
        if (this.statement == null)
            return new biz.c24.io.training.statements.Statement[]{};
        else
            return this.statement;
    }

    /**
     * Gets the index of <code>value</code> (0..*).
     * @param value The Statement to get the index of.
     * @return The index.
     **/
    public int getStatementIndex(biz.c24.io.training.statements.Statement value)
    {
        if (this.statement == null)
            return -1;
        for (int i=0; i<this.statement.length; i++)
            if (this.statement[i] == value)
                return i;
        return -1;
    }

    public int getTotalElementCount()
    {
        int count = 0;
        count += this.statement == null ? 0 : this.statement.length;
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
            case 0:
                removeStatement(index);
                return;
            default:
            super.removeElement(name, index);
        }
    }

    /**
     * Removes a Statement (0..*).
     * @param index The index of the Statement to get.
     **/
    public void removeStatement(int index)
    {
        if (this.statement == null)
            throw new java.lang.ArrayIndexOutOfBoundsException();
        biz.c24.io.training.statements.Statement[] temp = this.statement;
        this.statement = new biz.c24.io.training.statements.Statement[temp.length-1];
        java.lang.System.arraycopy(temp, 0, this.statement, 0, index);
        java.lang.System.arraycopy(temp, index+1, this.statement, index, temp.length-index-1);
        if (this.statement.length == 0)
            this.statement = null;
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
            case 0:
                if (this.statement == null)
                    throw new java.lang.ArrayIndexOutOfBoundsException();
                else if (value == null)
                    removeElement(name, index);
                else
                {
                    this.statement[index] = (biz.c24.io.training.statements.Statement) value;
                    ((biz.c24.io.api.data.ComplexDataObject) this.statement[index]).setParent(this, "Statement");
                }
                return;
            default:
                super.setElement(name, index, value);
        }
    }

    /**
     * Sets the value of Statement (0..*).
     * @param value The new value.
     **/
    public void setStatement(biz.c24.io.training.statements.Statement[] value)
    {
        this.statement = (biz.c24.io.training.statements.Statement[]) biz.c24.io.api.Utils.clearNulls(value);
        for (int i=0; this.statement != null && i<this.statement.length; i++)
            ((biz.c24.io.api.data.ComplexDataObject) this.statement[i]).setParent(this, "Statement");
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException
    {
        out.writeObject(this.statement);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException
    {
        this.statement = (biz.c24.io.training.statements.Statement[]) in.readObject();
    }

}
