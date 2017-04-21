/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package goobar.cextractor.jarmanager;

/**
 *
 * @author goobar
 */
@SuppressWarnings("javadoc")
public class CannotExtractArchiveException extends Exception
{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public CannotExtractArchiveException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
