package automata;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class AutomataDiagram extends JPanel {
	public AutomataDiagram() {
	}

	private static boolean evaluarCadena(String cadena) {
		String estado = "q0";
		for (int i = 0; i < cadena.length(); i++) {
			char c = cadena.charAt(i);
			if (estado.equals("q0")) {
				if (c == 'a') {
					estado = "q0";
				} else if (c == 'b' || c == 'c') {
					estado = "q1";
				} else {
					return false;
				}
			} else {
				if (c == 'a') {
					estado = "q1";
				} else {
					return false;
				}
			}
		}
		return estado.equals("q1");
	}

	private final Point q0 = new Point(220, 300);
	private final Point q1 = new Point(600, 300);
	private final int R = 40;

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(new BasicStroke(2f));
		g2.setFont(new Font("SansSerif", Font.BOLD, 16));

		g2.drawString("Autómata: ((a*b) U c) a*", 30, 40);

		drawStartArrow(g2, q0);
		drawSelfLoopTop(g2, q0, "a");
		drawCurve(g2, q0, q1, true, "b");
		drawCurve(g2, q0, q1, false, "c");
		drawSelfLoopTop(g2, q1, "a");

		drawState(g2, q0, "q0", false);
		drawState(g2, q1, "q1", true);
	}

	private void drawState(Graphics2D g2, Point c, String name, boolean accepting) {
		g2.setColor(Color.WHITE);
		g2.fillOval(c.x - R, c.y - R, 2 * R, 2 * R);
		g2.setColor(Color.BLACK);
		g2.drawOval(c.x - R, c.y - R, 2 * R, 2 * R);
		if (accepting) {
			int r2 = R - 6;
			g2.drawOval(c.x - r2, c.y - r2, 2 * r2, 2 * r2);
		}
		FontMetrics fm = g2.getFontMetrics();
		int tw = fm.stringWidth(name);
		g2.drawString(name, c.x - tw / 2, c.y + fm.getAscent() / 2 - 2);
	}

	private void drawStartArrow(Graphics2D g2, Point target) {
		int x0 = target.x - R - 60;
		int y0 = target.y;
		int x1 = target.x - R;
		int y1 = target.y;
		g2.draw(new Line2D.Double(x0, y0, x1, y1));
		drawArrowHead(g2, new Point2D.Double(x1, y1), Math.atan2(y1 - y0, x1 - x0));
	}

	private void drawSelfLoopTop(Graphics2D g2, Point c, String label) {
		int loopW = 70;
		int loopH = 70;
		int x = c.x - loopW / 2;
		int y = c.y - R - loopH + 15;
		Arc2D.Double arc = new Arc2D.Double(x, y, loopW, loopH, 20, 320, Arc2D.OPEN);
		g2.draw(arc);

		double endAngleDeg = 20;
		Point2D endPoint = pointOnArc(arc, endAngleDeg);
		Point2D nearPoint = pointOnArc(arc, endAngleDeg + 8);
		double angle = Math.atan2(endPoint.getY() - nearPoint.getY(), endPoint.getX() - nearPoint.getX());
		drawArrowHead(g2, endPoint, angle);

		g2.drawString(label, c.x - 5, y - 8);
	}

	private Point2D pointOnArc(Arc2D.Double arc, double angleDeg) {
		double rad = Math.toRadians(angleDeg);
		double cx = arc.getCenterX();
		double cy = arc.getCenterY();
		double rx = arc.getWidth() / 2;
		double ry = arc.getHeight() / 2;
		double px = cx + rx * Math.cos(rad);
		double py = cy - ry * Math.sin(rad);
		return new Point2D.Double(px, py);
	}

	private void drawCurve(Graphics2D g2, Point from, Point to, boolean above, String label) {
		int midX = (from.x + to.x) / 2;
		int offset = above ? -90 : 90;
		int ctrlY = from.y + offset;

		double dirAngle = above ? Math.toRadians(-35) : Math.toRadians(35);
		Point2D start = new Point2D.Double(from.x + R * Math.cos(dirAngle),
				from.y - R * Math.sin(dirAngle) * (above ? 1 : -1));

		double dirAngleEnd = above ? Math.toRadians(215) : Math.toRadians(145);
		Point2D end = new Point2D.Double(to.x + R * Math.cos(dirAngleEnd),
				to.y - R * Math.sin(dirAngleEnd) * (above ? 1 : -1));

		QuadCurve2D.Double curve = new QuadCurve2D.Double(start.getX(), start.getY(), midX, ctrlY, end.getX(),
				end.getY());
		g2.draw(curve);

		double angle = Math.atan2(end.getY() - ctrlY, end.getX() - midX);
		drawArrowHead(g2, end, angle);

		g2.drawString(label, midX - 5, ctrlY + (above ? -8 : 20));
	}

	private void drawArrowHead(Graphics2D g2, Point2D tip, double angle) {
		int len = 12;
		double a1 = angle + Math.toRadians(150);
		double a2 = angle - Math.toRadians(150);
		Point2D p1 = new Point2D.Double(tip.getX() + len * Math.cos(a1), tip.getY() + len * Math.sin(a1));
		Point2D p2 = new Point2D.Double(tip.getX() + len * Math.cos(a2), tip.getY() + len * Math.sin(a2));
		Polygon head = new Polygon();
		head.addPoint((int) tip.getX(), (int) tip.getY());
		head.addPoint((int) p1.getX(), (int) p1.getY());
		head.addPoint((int) p2.getX(), (int) p2.getY());
		g2.fill(head);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Automata NFA - ((a*b) U c)a*");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(850, 620);
			frame.setLayout(new BorderLayout());
			frame.getContentPane().add(new AutomataDiagram(), BorderLayout.CENTER);

			JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JTextField cadenaField = new JTextField(15);
			JButton validarBtn = new JButton("Validar cadena");
			JLabel resultadoLabel = new JLabel(" ");
			resultadoLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

			validarBtn.addActionListener(e -> {
				String cadena = cadenaField.getText();
				boolean aceptada = evaluarCadena(cadena);
				if (aceptada) {
					resultadoLabel.setForeground(new Color(0, 128, 0));
					resultadoLabel.setText("\"" + cadena + "\" es VALIDA");
				} else {
					resultadoLabel.setForeground(Color.RED);
					resultadoLabel.setText("\"" + cadena + "\" es INVALIDA");
				}
			});

			controlPanel.add(new JLabel("Cadena:"));
			controlPanel.add(cadenaField);
			controlPanel.add(validarBtn);
			controlPanel.add(resultadoLabel);

			frame.getContentPane().add(controlPanel, BorderLayout.SOUTH);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}