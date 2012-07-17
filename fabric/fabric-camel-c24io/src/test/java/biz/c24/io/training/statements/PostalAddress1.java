package biz.c24.io.training.statements;

import java.util.Arrays;

/**
 * PostalAddress1. <p/>
 * This object is composed of the following <i>elements</i>:
 * <ul>
 * <li><b>AdrTp</b> of type {@link java.lang.String} (0..1)</li>
 * <li><b>AdrLine</b> of type {@link java.lang.String} (0..5)</li>
 * <li><b>StrtNm</b> of type {@link java.lang.String} (0..1)</li>
 * <li><b>BldgNb</b> of type {@link java.lang.String} (0..1)</li>
 * <li><b>PstCd</b> of type {@link java.lang.String} (0..1)</li>
 * <li><b>TwnNm</b> of type {@link java.lang.String} (0..1)</li>
 * <li><b>CtrySubDvsn</b> of type {@link java.lang.String} (0..1)</li>
 * <li><b>Ctry</b> of type {@link java.lang.String} (1)</li>
 * </ul>
 * @author C24 Integration Objects;
 * @see biz.c24.io.training.statements.PostalAddress1Class
 **/
public class PostalAddress1 extends biz.c24.io.api.data.ComplexDataObject 
{
    private static final java.lang.String[] NATIVE_ELEMENTS = new String[] {"AdrLine", "AdrTp", "BldgNb", "Ctry", "CtrySubDvsn", "PstCd", "StrtNm", "TwnNm"};
    private java.lang.String adrTp;
    private java.lang.String[] adrLine;
    private java.lang.String strtNm;
    private java.lang.String bldgNb;
    private java.lang.String pstCd;
    private java.lang.String twnNm;
    private java.lang.String ctrySubDvsn;
    private java.lang.String ctry;

    /**
     * Constructs a new instance defined by the default element.
     **/
    public PostalAddress1()
    {
        this(biz.c24.io.training.statements.PostalAddress1Class.getInstance().getNullDefiningElementDecl());
    }

    /**
     * Constructs a new instance defined by the specified element.
     * @param definingElementDecl The element which defines the object.
     **/
    public PostalAddress1(biz.c24.io.api.data.Element definingElementDecl)
    {
        super(definingElementDecl);
    }

    /**
     * Constructs a new instance defined by the specified element and type.
     * @param definingElementDecl The element which defines the object.
     * @param type The type which defines the object.
     **/
    public PostalAddress1(biz.c24.io.api.data.Element definingElementDecl, biz.c24.io.api.data.ComplexDataType type)
    {
        super(definingElementDecl, type);
    }

    /**
     * Constructs a new instance cloned from the specified object.
     * @param clone The object to be cloned.
     **/
    public PostalAddress1(biz.c24.io.training.statements.PostalAddress1 clone)
    {
        super(clone);
    }

    /**
     * Adds a AdrLine (0..5).
     * @param value The new AdrLine.
     **/
    public void addAdrLine(java.lang.String value)
    {
        java.lang.String[] temp = this.adrLine;
        this.adrLine = new java.lang.String[temp == null ? 1 : (temp.length+1)];
        if (temp != null)
            java.lang.System.arraycopy(temp, 0, this.adrLine, 0, temp.length);
        this.adrLine[this.adrLine.length-1] = value;
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
                setAdrTp((java.lang.String) value);
                return;
            case 0:
                addAdrLine((java.lang.String) value);
                return;
            case 6:
                setStrtNm((java.lang.String) value);
                return;
            case 2:
                setBldgNb((java.lang.String) value);
                return;
            case 5:
                setPstCd((java.lang.String) value);
                return;
            case 7:
                setTwnNm((java.lang.String) value);
                return;
            case 4:
                setCtrySubDvsn((java.lang.String) value);
                return;
            case 3:
                setCtry((java.lang.String) value);
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
        return new biz.c24.io.training.statements.PostalAddress1(this);
    }

    /**
     * Creates and returns a deep clone of this object.
     * @return The new object.
     * @see #clone()
     **/
    public biz.c24.io.api.data.ComplexDataObject cloneDeep() throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.api.data.ComplexDataObject obj = new biz.c24.io.training.statements.PostalAddress1(this);
        cloneDeep(obj);
        return obj;
    }

    protected void cloneDeep(biz.c24.io.api.data.ComplexDataObject clone) throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.training.statements.PostalAddress1 obj = (biz.c24.io.training.statements.PostalAddress1) clone;
        obj.adrTp = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.adrTp, obj, "AdrTp");
        obj.adrLine = (java.lang.String[]) biz.c24.io.api.Utils.cloneDeep(this.adrLine, obj, "AdrLine");
        obj.strtNm = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.strtNm, obj, "StrtNm");
        obj.bldgNb = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.bldgNb, obj, "BldgNb");
        obj.pstCd = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.pstCd, obj, "PstCd");
        obj.twnNm = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.twnNm, obj, "TwnNm");
        obj.ctrySubDvsn = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.ctrySubDvsn, obj, "CtrySubDvsn");
        obj.ctry = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.ctry, obj, "Ctry");
    }

    public boolean equals(java.lang.Object obj)
    {
        if(obj instanceof biz.c24.io.api.data.ComplexDataObject)
            return equalContents((biz.c24.io.api.data.ComplexDataObject) obj, true, true, true, true);
        else
            return obj.equals(this);
    }

    /**
     * Gets the value of AdrLine (0..5).
     * @return The value.
     **/
    public java.lang.String[] getAdrLine()
    {
        if (this.adrLine == null)
            return new java.lang.String[]{};
        else
            return this.adrLine;
    }

    /**
     * Gets the index of <code>value</code> (0..5).
     * @param value The AdrLine to get the index of.
     * @return The index.
     **/
    public int getAdrLineIndex(java.lang.String value)
    {
        if (this.adrLine == null)
            return -1;
        for (int i=0; i<this.adrLine.length; i++)
            if (this.adrLine[i] == value)
                return i;
        return -1;
    }

    /**
     * Gets the value of AdrTp (0..1).
     * @return The value.
     **/
    public java.lang.String getAdrTp()
    {
        return this.adrTp;
    }

    /**
     * Gets the value of BldgNb (0..1).
     * @return The value.
     **/
    public java.lang.String getBldgNb()
    {
        return this.bldgNb;
    }

    /**
     * Gets the value of Ctry (1).
     * @return The value.
     **/
    public java.lang.String getCtry()
    {
        return this.ctry;
    }

    /**
     * Gets the value of CtrySubDvsn (0..1).
     * @return The value.
     **/
    public java.lang.String getCtrySubDvsn()
    {
        return this.ctrySubDvsn;
    }

    /**
     * Returns the element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are: <b>adrTp, adrLine, strtNm, bldgNb, pstCd, twnNm, ctrySubDvsn, ctry</b>.
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
                return this.adrTp;
            case 0:
            if (this.adrLine == null)
                throw new java.lang.ArrayIndexOutOfBoundsException();
            else
                return this.adrLine[index];
            case 6:
                return this.strtNm;
            case 2:
                return this.bldgNb;
            case 5:
                return this.pstCd;
            case 7:
                return this.twnNm;
            case 4:
                return this.ctrySubDvsn;
            case 3:
                return this.ctry;
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
                return this.adrTp == null ? 0 : 1;
            case 0:
                return this.adrLine == null ? 0 : this.adrLine.length;
            case 6:
                return this.strtNm == null ? 0 : 1;
            case 2:
                return this.bldgNb == null ? 0 : 1;
            case 5:
                return this.pstCd == null ? 0 : 1;
            case 7:
                return this.twnNm == null ? 0 : 1;
            case 4:
                return this.ctrySubDvsn == null ? 0 : 1;
            case 3:
                return this.ctry == null ? 0 : 1;
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
                return this.adrTp != null && this.adrTp.equals(element) ? 0 : -1;
            case 0:
                return getAdrLineIndex((java.lang.String) element);
            case 6:
                return this.strtNm != null && this.strtNm.equals(element) ? 0 : -1;
            case 2:
                return this.bldgNb != null && this.bldgNb.equals(element) ? 0 : -1;
            case 5:
                return this.pstCd != null && this.pstCd.equals(element) ? 0 : -1;
            case 7:
                return this.twnNm != null && this.twnNm.equals(element) ? 0 : -1;
            case 4:
                return this.ctrySubDvsn != null && this.ctrySubDvsn.equals(element) ? 0 : -1;
            case 3:
                return this.ctry != null && this.ctry.equals(element) ? 0 : -1;
            default:
                return super.getElementIndex(name, element);
        }
    }

    /**
     * Gets the value of PstCd (0..1).
     * @return The value.
     **/
    public java.lang.String getPstCd()
    {
        return this.pstCd;
    }

    /**
     * Gets the value of StrtNm (0..1).
     * @return The value.
     **/
    public java.lang.String getStrtNm()
    {
        return this.strtNm;
    }

    public int getTotalElementCount()
    {
        int count = 0;
        count += this.adrTp == null ? 0 : 1;
        count += this.adrLine == null ? 0 : this.adrLine.length;
        count += this.strtNm == null ? 0 : 1;
        count += this.bldgNb == null ? 0 : 1;
        count += this.pstCd == null ? 0 : 1;
        count += this.twnNm == null ? 0 : 1;
        count += this.ctrySubDvsn == null ? 0 : 1;
        count += this.ctry == null ? 0 : 1;
        return count;
    }

    /**
     * Gets the value of TwnNm (0..1).
     * @return The value.
     **/
    public java.lang.String getTwnNm()
    {
        return this.twnNm;
    }

    public int hashCode()
    {
        return this.toString().length();
    }

    /**
     * Removes a AdrLine (0..5).
     * @param index The index of the AdrLine to get.
     **/
    public void removeAdrLine(int index)
    {
        if (this.adrLine == null)
            throw new java.lang.ArrayIndexOutOfBoundsException();
        java.lang.String[] temp = this.adrLine;
        this.adrLine = new java.lang.String[temp.length-1];
        java.lang.System.arraycopy(temp, 0, this.adrLine, 0, index);
        java.lang.System.arraycopy(temp, index+1, this.adrLine, index, temp.length-index-1);
        if (this.adrLine.length == 0)
            this.adrLine = null;
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
                setAdrTp(null);
                return;
            case 0:
                removeAdrLine(index);
                return;
            case 6:
                setStrtNm(null);
                return;
            case 2:
                setBldgNb(null);
                return;
            case 5:
                setPstCd(null);
                return;
            case 7:
                setTwnNm(null);
                return;
            case 4:
                setCtrySubDvsn(null);
                return;
            case 3:
                setCtry(null);
                return;
            default:
            super.removeElement(name, index);
        }
    }

    /**
     * Sets the value of AdrLine (0..5).
     * @param value The new value.
     **/
    public void setAdrLine(java.lang.String[] value)
    {
        this.adrLine = (java.lang.String[]) biz.c24.io.api.Utils.clearNulls(value);
    }

    /**
     * Sets the value of AdrTp (0..1).
     * @param value The new value.
     **/
    public void setAdrTp(java.lang.String value)
    {
        this.adrTp = value;
    }

    /**
     * Sets the value of BldgNb (0..1).
     * @param value The new value.
     **/
    public void setBldgNb(java.lang.String value)
    {
        this.bldgNb = value;
    }

    /**
     * Sets the value of Ctry (1).
     * @param value The new value.
     **/
    public void setCtry(java.lang.String value)
    {
        this.ctry = value;
    }

    /**
     * Sets the value of CtrySubDvsn (0..1).
     * @param value The new value.
     **/
    public void setCtrySubDvsn(java.lang.String value)
    {
        this.ctrySubDvsn = value;
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
                setAdrTp((java.lang.String) value);
                return;
            case 0:
                if (this.adrLine == null)
                    throw new java.lang.ArrayIndexOutOfBoundsException();
                else if (value == null)
                    removeElement(name, index);
                else
                this.adrLine[index] = (java.lang.String) value;
                return;
            case 6:
                setStrtNm((java.lang.String) value);
                return;
            case 2:
                setBldgNb((java.lang.String) value);
                return;
            case 5:
                setPstCd((java.lang.String) value);
                return;
            case 7:
                setTwnNm((java.lang.String) value);
                return;
            case 4:
                setCtrySubDvsn((java.lang.String) value);
                return;
            case 3:
                setCtry((java.lang.String) value);
                return;
            default:
                super.setElement(name, index, value);
        }
    }

    /**
     * Sets the value of PstCd (0..1).
     * @param value The new value.
     **/
    public void setPstCd(java.lang.String value)
    {
        this.pstCd = value;
    }

    /**
     * Sets the value of StrtNm (0..1).
     * @param value The new value.
     **/
    public void setStrtNm(java.lang.String value)
    {
        this.strtNm = value;
    }

    /**
     * Sets the value of TwnNm (0..1).
     * @param value The new value.
     **/
    public void setTwnNm(java.lang.String value)
    {
        this.twnNm = value;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException
    {
        out.writeObject(this.adrTp);
        out.writeObject(this.adrLine);
        out.writeObject(this.strtNm);
        out.writeObject(this.bldgNb);
        out.writeObject(this.pstCd);
        out.writeObject(this.twnNm);
        out.writeObject(this.ctrySubDvsn);
        out.writeObject(this.ctry);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException
    {
        this.adrTp = (java.lang.String) in.readObject();
        this.adrLine = (java.lang.String[]) in.readObject();
        this.strtNm = (java.lang.String) in.readObject();
        this.bldgNb = (java.lang.String) in.readObject();
        this.pstCd = (java.lang.String) in.readObject();
        this.twnNm = (java.lang.String) in.readObject();
        this.ctrySubDvsn = (java.lang.String) in.readObject();
        this.ctry = (java.lang.String) in.readObject();
    }

}
