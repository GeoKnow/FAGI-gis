package gr.athenainnovation.imis.fusion.gis.gui.listeners;

/**
 * Error message listener.
 * @author Thomas Maroulis
 */
public interface ErrorListener {
    /**
     * Notify listener of error message.
     * @param message error message
     */
    void notifyError(final String message);
}
