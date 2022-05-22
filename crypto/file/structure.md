Encrypted File Structure
========================

Major requirements:

- random access to file plaintext must be supported,

The encrypted file has following structure:

1. string `ArchiveKeep Encrypted File` followed by byte `0`,
2. file format - 8bit integer - currently constant `1`,
3. metadata section length - 32bit integer, 
4. metadata section contents:
   1. public metadata length - 32bit integer (can be zero), 
   2. public metadata contents (optional),
   3. private metadata length - 32bit integer (greater than zero),
   4. private metadata contents (mandatory),
   5. null-terminator (or else meaningful constant indicating follow-up contents),
   6. random data to fill the unused space (optional),
5. encrypted file data. 

All integers are stored in big endian system.

