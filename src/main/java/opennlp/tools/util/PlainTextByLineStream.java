/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package opennlp.tools.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

/**
 * Reads a plain text file and return each line as a <code>String</code> object.
 */
public class PlainTextByLineStream implements ObjectStream<String> {
  
  private FileChannel channel;
  private String encoding;
  
  private BufferedReader in;
  
  /**
   * Initializes the current instance.
   * 
   * @param in
   */
  public PlainTextByLineStream(Reader in) {
    this.in = new BufferedReader(in);
  }
  
  public PlainTextByLineStream(FileChannel channel, String encoding) {
    this.encoding = encoding;
    this.channel = channel;
    
    in = new BufferedReader(Channels.newReader(channel, encoding));
  }
  
  public String read() throws ObjectStreamException {
    try {
      return in.readLine();
    } catch (IOException e) {
      throw new ObjectStreamException(e);
    }
  }

  public void reset() throws ObjectStreamException {
    
    try {
      if (channel == null) {
          in.reset();
      }
      else {
        channel.position(0);
        in = new BufferedReader(Channels.newReader(channel, encoding));
      }
    } catch (IOException e) {
      throw new ObjectStreamException(e);
    }
  }
  
  public void close() throws ObjectStreamException {
    try {
      if (channel == null) {
        in.close();
      }
      else {
       channel.close(); 
      }
    }
    catch (IOException e) {
      throw new ObjectStreamException(e);
    }
  }
}
