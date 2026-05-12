package math;

public class Vector2D {
    public double x;
    public double y;

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void add(Vector2D v) {
        this.x += v.x;
        this.y += v.y;
    }

    public void sub(Vector2D v) {
        this.x -= v.x;
        this.y -= v.y;
    }

    public void mult(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
    }

    public double mag() {
        return Math.sqrt(x * x + y * y);
    }

    public void normalize() {
        double m = mag();
        if (m != 0) {
            mult(1 / m);
        }
    }

    // Static helper methods
    public static Vector2D sub(Vector2D v1, Vector2D v2) {
        return new Vector2D(v1.x - v2.x, v1.y - v2.y);
    }

    public static Vector2D add(Vector2D v1, Vector2D v2) {
        return new Vector2D(v1.x + v2.x, v1.y + v2.y);
    }

    public Vector2D copy() {
        return new Vector2D(this.x, this.y);
    }
}
