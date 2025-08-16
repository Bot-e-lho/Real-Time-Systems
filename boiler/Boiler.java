package boiler;


public class Boiler {
   public final double C = 1000.0;
   public final double M1 = 150.0;
   public final double M2 = 850.0;
   public final double N1 = 400.0;
   public final double N2 = 600.0;
   public final double V = 10.0; // é 70 mas coloquei 35 pra testar
   public final double P = 25.0; // é 50 mas coloquei 25 pra testar
   private double q;
   private double p;
   private double v;

   public Boiler(double var1) {
      this.q = this.clamp(var1, 0.0, 1000.0);
      this.p = 0.0;
      this.v = 0.0;
   }

   public synchronized void setPumpThroughput(double p) {
      this.p = p;
   }

   public synchronized void updateOneSecond() {
        double currentVaporFlow = (q > 0) ? this.v : 0.0;
        
        double delta = (p - currentVaporFlow);
        q = clamp(q + delta, 0.0, C);
    }

   private double clamp(double var1, double var3, double var5) {
      if (var1 < var3) {
        return var3;
      } 
      if (var1 > var5) {
        return var5;
      }
      return var1;
   }

   public synchronized void setVaporThroughput(double newV) {
        this.v = clamp(newV, 0.0, V); 
    }

   public synchronized double getQ() {
      return q;
   }

   public synchronized double getP() {
      return p;
   }

   public synchronized void drain(double amount) { 
      q = clamp(q - amount, 0.0, C); 
   }


   public double getV() {
      return v;
   }

   public synchronized boolean isUnderMin() {
      return q <= M1;
   }

   public synchronized boolean isOverMax() {
      return q >= M2;
   }

   public synchronized boolean isInNormal() {
      return q >= N1 && q <= N2;
   }
}
