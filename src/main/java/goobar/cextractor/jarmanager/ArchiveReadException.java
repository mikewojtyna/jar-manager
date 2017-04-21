/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package goobar.cextractor.jarmanager;

/**
 * Thrown when archive can't be read.
 *
 * @author goobar
 */
public class ArchiveReadException extends Exception
{
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance of <code>ArchiveReadException</code> without
	 * detail message.
	 */
	public ArchiveReadException()
	{
	}

	/**
	 * Constructs an instance of <code>ArchiveReadException</code> with the
	 * specified detail message.
	 *
	 * @param msg
	 *                the detail message.
	 */
	public ArchiveReadException(String msg)
	{
		super(msg);
	}
}
