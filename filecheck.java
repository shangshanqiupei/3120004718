import java.util.Scanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class filecheck
{
    public static void main(String[] args) throws IOException
    {

        //获取到论文的信息
        Scanner s = new Scanner(System.in);
        String rode = s.next();
        File file=new File(rode);

        InputStream in=new FileInputStream(file);
        byte[] b=new byte[in.available()];
        in.read(b);
        String file1=new String(b,"GBK");
        String rode2 = s.next();
        //获取论文库的信息
        File dir=new File(rode2);
        File[] listFiles = dir.listFiles();
        for(int i=0;i<listFiles.length;i++)
        {
            File f=listFiles[i];
            InputStream in1=new FileInputStream(f);
            byte[] b1=new byte[in1.available()];
            in1.read(b1);
            String file2=new String(b1,"GBK");
            SimHash hash1 = new SimHash(file1, 64);
            SimHash hash2 = new SimHash(file2, 64);
            int dis12 = hash1.getDistance(hash1.strSimHash, hash2.strSimHash);
            System.out.println((1-(double)dis12/64));
        }

    }

}
