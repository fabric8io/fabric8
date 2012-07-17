package biz.c24.io.training.statements;

import java.util.Arrays;

/**
 * Statement. <p/>
 * This object is composed of the following <i>elements</i>:
 * <ul>
 * <li><b>Hdr</b> of type {@link biz.c24.io.training.statements.Header} (1)</li>
 * <li><b>StmtLine</b> of type {@link biz.c24.io.training.statements.StatementLine} (0..*)</li>
 * <li><b>Tlr</b> of type {@link biz.c24.io.training.statements.Trailer} (1)</li>
 * </ul>
 * @author C24 Integration Objects;
 * @see biz.c24.io.training.statements.StatementClass
 **/
public class Statement extends biz.c24.io.api.data.ComplexDataObject 
{
    private static final java.lang.String[] NATIVE_ELEMENTS = new String[] {"Hdr", "StmtLine", "Tlr"};
    private biz.c24.io.training.statements.Header hdr;
    private biz.c24.io.training.statements.StatementLine[] stmtLine;
    private biz.c24.io.training.statements.Trailer tlr;

    /**
     * Constructs a new instance defined by the default element.
     **/
    public Statement()
    {
        this(biz.c24.io.training.statements.StatementClass.getInstance().getNullDefiningElementDecl());
    }

    /**
     * Constructs a new instance defined by the specified element.
     * @param definingElementDecl The element which defines the object.
     **/
    public Statement(biz.c24.io.api.data.Element definingElementDecl)
    {
        super(definingElementDecl);
    }

    /**
     * Constructs a new instance defined by the specified element and type.
     * @param definingElementDecl The element which defines the object.
     * @param type The type which defines the object.
     **/
    public Statement(biz.c24.io.api.data.Element definingElementDecl, biz.c24.io.api.data.ComplexDataType type)
    {
        super(definingElementDecl, type);
    }

    /**
     * Constructs a new instance cloned from the specified object.
     * @param clone The object to be cloned.
     **/
    public Statement(biz.c24.io.training.statements.Statement clone)
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
                setHdr((biz.c24.io.training.statements.Header) value);
                return;
            case 1:
                addStmtLine((biz.c24.io.training.statements.StatementLine) value);
                return;
            case 2:
                setTlr((biz.c24.io.training.statements.Trailer) value);
                return;
            default:
                super.addElement(name, value);
        }
    }

    /**
     * Adds a StmtLine (0..*).
     * @param value The new StmtLine.
     **/
    public void addStmtLine(biz.c24.io.training.statements.StatementLine value)
    {
        biz.c24.io.training.statements.StatementLine[] temp = this.stmtLine;
        this.stmtLine = new biz.c24.io.training.statements.StatementLine[temp == null ? 1 : (temp.length+1)];
        if (temp != null)
            java.lang.System.arraycopy(temp, 0, this.stmtLine, 0, temp.length);
        this.stmtLine[this.stmtLine.length-1] = value;
        ((biz.c24.io.api.data.ComplexDataObject) value).setParent(this, "StmtLine");
    }

    /**
     * Creates and returns a shallow clone of this object.
     * @see #cloneDeep()
     **/
    public java.lang.Object clone()
    {
        return new biz.c24.io.training.statements.Statement(this);
    }

    /**
     * Creates and returns a deep clone of this object.
     * @return The new object.
     * @see #clone()
     **/
    public biz.c24.io.api.data.ComplexDataObject cloneDeep() throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.api.data.ComplexDataObject obj = new biz.c24.io.training.statements.Statement(this);
        cloneDeep(obj);
        return obj;
    }

    protected void cloneDeep(biz.c24.io.api.data.ComplexDataObject clone) throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.training.statements.Statement obj = (biz.c24.io.training.statements.Statement) clone;
        obj.hdr = (biz.c24.io.training.statements.Header) biz.c24.io.api.Utils.cloneDeep(this.hdr, obj, "Hdr");
        obj.stmtLine = (biz.c24.io.training.statements.StatementLine[]) biz.c24.io.api.Utils.cloneDeep(this.stmtLine, obj, "StmtLine");
        obj.tlr = (biz.c24.io.training.statements.Trailer) biz.c24.io.api.Utils.cloneDeep(this.tlr, obj, "Tlr");
    }

    /**
     * Creates, adds and returns a new Hdr (1).
     * @return The new value.
     **/
    public biz.c24.io.training.statements.Header createHdr()
    {
        biz.c24.io.training.statements.Header obj = (biz.c24.io.training.statements.Header) getElementDecl("Hdr").createObject();
        setHdr(obj);
        return obj;
    }

    /**
     * Creates, adds and returns a new StmtLine (0..*).
     * @return The new value.
     **/
    public biz.c24.io.training.statements.StatementLine createStmtLine()
    {
        biz.c24.io.training.statements.StatementLine obj = (biz.c24.io.training.statements.StatementLine) getElementDecl("StmtLine").createObject();
        addStmtLine(obj);
        return obj;
    }

    /**
     * Creates, adds and returns a new Tlr (1).
     * @return The new value.
     **/
    public biz.c24.io.training.statements.Trailer createTlr()
    {
        biz.c24.io.training.statements.Trailer obj = (biz.c24.io.training.statements.Trailer) getElementDecl("Tlr").createObject();
        setTlr(obj);
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
     * The legal value(s) for <code>name</code> are: <b>hdr, stmtLine, tlr</b>.
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
                return this.hdr;
            case 1:
            if (this.stmtLine == null)
                throw new java.lang.ArrayIndexOutOfBoundsException();
            else
                return this.stmtLine[index];
            case 2:
                return this.tlr;
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
                return this.hdr == null ? 0 : 1;
            case 1:
                return this.stmtLine == null ? 0 : this.stmtLine.length;
            case 2:
                return this.tlr == null ? 0 : 1;
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
                return this.hdr != null && this.hdr.equals(element) ? 0 : -1;
            case 1:
                return getStmtLineIndex((biz.c24.io.training.statements.StatementLine) element);
            case 2:
                return this.tlr != null && this.tlr.equals(element) ? 0 : -1;
            default:
                return super.getElementIndex(name, element);
        }
    }

    /**
     * Gets the value of Hdr (1).
     * @return The value.
     **/
    public biz.c24.io.training.statements.Header getHdr()
    {
        return this.hdr;
    }

    /**
     * Gets the value of StmtLine (0..*).
     * @return The value.
     **/
    public biz.c24.io.training.statements.StatementLine[] getStmtLine()
    {
        if (this.stmtLine == null)
            return new biz.c24.io.training.statements.StatementLine[]{};
        else
            return this.stmtLine;
    }

    /**
     * Gets the index of <code>value</code> (0..*).
     * @param value The StmtLine to get the index of.
     * @return The index.
     **/
    public int getStmtLineIndex(biz.c24.io.training.statements.StatementLine value)
    {
        if (this.stmtLine == null)
            return -1;
        for (int i=0; i<this.stmtLine.length; i++)
            if (this.stmtLine[i] == value)
                return i;
        return -1;
    }

    /**
     * Gets the value of Tlr (1).
     * @return The value.
     **/
    public biz.c24.io.training.statements.Trailer getTlr()
    {
        return this.tlr;
    }

    public int getTotalElementCount()
    {
        int count = 0;
        count += this.hdr == null ? 0 : 1;
        count += this.stmtLine == null ? 0 : this.stmtLine.length;
        count += this.tlr == null ? 0 : 1;
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
                setHdr(null);
                return;
            case 1:
                removeStmtLine(index);
                return;
            case 2:
                setTlr(null);
                return;
            default:
            super.removeElement(name, index);
        }
    }

    /**
     * Removes a StmtLine (0..*).
     * @param index The index of the StmtLine to get.
     **/
    public void removeStmtLine(int index)
    {
        if (this.stmtLine == null)
            throw new java.lang.ArrayIndexOutOfBoundsException();
        biz.c24.io.training.statements.StatementLine[] temp = this.stmtLine;
        this.stmtLine = new biz.c24.io.training.statements.StatementLine[temp.length-1];
        java.lang.System.arraycopy(temp, 0, this.stmtLine, 0, index);
        java.lang.System.arraycopy(temp, index+1, this.stmtLine, index, temp.length-index-1);
        if (this.stmtLine.length == 0)
            this.stmtLine = null;
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
                setHdr((biz.c24.io.training.statements.Header) value);
                return;
            case 1:
                if (this.stmtLine == null)
                    throw new java.lang.ArrayIndexOutOfBoundsException();
                else if (value == null)
                    removeElement(name, index);
                else
                {
                    this.stmtLine[index] = (biz.c24.io.training.statements.StatementLine) value;
                    ((biz.c24.io.api.data.ComplexDataObject) this.stmtLine[index]).setParent(this, "StmtLine");
                }
                return;
            case 2:
                setTlr((biz.c24.io.training.statements.Trailer) value);
                return;
            default:
                super.setElement(name, index, value);
        }
    }

    /**
     * Sets the value of Hdr (1).
     * @param value The new value.
     **/
    public void setHdr(biz.c24.io.training.statements.Header value)
    {
        this.hdr = value;
        if (this.hdr != null)
            ((biz.c24.io.api.data.ComplexDataObject) this.hdr).setParent(this, "Hdr");
    }

    /**
     * Sets the value of StmtLine (0..*).
     * @param value The new value.
     **/
    public void setStmtLine(biz.c24.io.training.statements.StatementLine[] value)
    {
        this.stmtLine = (biz.c24.io.training.statements.StatementLine[]) biz.c24.io.api.Utils.clearNulls(value);
        for (int i=0; this.stmtLine != null && i<this.stmtLine.length; i++)
            ((biz.c24.io.api.data.ComplexDataObject) this.stmtLine[i]).setParent(this, "StmtLine");
    }

    /**
     * Sets the value of Tlr (1).
     * @param value The new value.
     **/
    public void setTlr(biz.c24.io.training.statements.Trailer value)
    {
        this.tlr = value;
        if (this.tlr != null)
            ((biz.c24.io.api.data.ComplexDataObject) this.tlr).setParent(this, "Tlr");
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException
    {
        out.writeObject(this.hdr);
        out.writeObject(this.stmtLine);
        out.writeObject(this.tlr);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException
    {
        this.hdr = (biz.c24.io.training.statements.Header) in.readObject();
        this.stmtLine = (biz.c24.io.training.statements.StatementLine[]) in.readObject();
        this.tlr = (biz.c24.io.training.statements.Trailer) in.readObject();
    }

}
