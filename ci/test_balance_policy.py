"""Exercise production pure policy methods at progression boundaries with Java 17."""
from pathlib import Path
import re, subprocess, tempfile
root=Path(__file__).parent/'m616'
slots=re.search(r'public static int slots\(int level\) \{[^}]+\}',(root/'KeystoneLimit.java').read_text()).group()
curve=re.search(r'static double curve\(double l\) \{[^}]+\}',(root/'WorldScalingEvents.java').read_text()).group()
source='public class PolicyCheck {\n'+slots+'\n'+curve+'''
public static void main(String[] args) {
 int[][] cases={{0,0},{39,0},{40,1},{59,1},{60,2},{79,2},{80,3},{150,3},{1000,3}};
 for(int[] c:cases) if(slots(c[0])!=c[1]) throw new AssertionError("Slot threshold "+c[0]);
 double previous=0;
 for(int l=0;l<=200;l++){double c=curve(l);if(c<previous||c<0||c>1)throw new AssertionError("Curve "+l);previous=c;}
 if(curve(10)!=0||Math.abs(curve(150)-1)>1e-9)throw new AssertionError("Curve endpoints");
 System.out.println("PASS: slot boundaries, cap, monotonic scaling and level-150 endpoint");
}}
'''
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp)/'PolicyCheck.java';p.write_text(source)
 subprocess.run(['javac',str(p)],check=True)
 subprocess.run(['java','-cp',tmp,'PolicyCheck'],check=True)
