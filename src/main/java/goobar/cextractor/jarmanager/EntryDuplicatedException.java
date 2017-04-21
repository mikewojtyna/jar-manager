/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package goobar.cextractor.jarmanager;

/**
 * Exception thrown when specified entry doesn't exist in archive.
 * 
 * @author goobar
 */
public class EntryDuplicatedException extends Exception
{
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance of <code>EntryDuplicatedException</code>
	 * without detail message.
	 */
	public EntryDuplicatedException()
	{
	}

	/**
	 * Constructs an instance of <code>EntryDuplicatedException</code> with
	 * the specified detail message.
	 *
	 * @param msg
	 *                the detail message.
	 */
	public EntryDuplicatedException(String msg)
	{
		super(msg);
	}
}
