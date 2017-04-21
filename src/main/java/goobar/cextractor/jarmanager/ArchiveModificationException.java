/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package goobar.cextractor.jarmanager;

/**
 * Exception thrown when archive can't be modified (for example due to bad
 * permissions, file nonexistence etc.)
 *
 * @author goobar
 */
@SuppressWarnings("javadoc")
public class ArchiveModificationException extends Exception
{
	private static final long serialVersionUID = 1L;

	public ArchiveModificationException()
	{
	}

	public ArchiveModificationException(String msg)
	{
		super(msg);
	}

	public ArchiveModificationException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
