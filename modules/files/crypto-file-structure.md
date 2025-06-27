Encrypted File Structure
========================

Major requirements:

- random access to file plaintext must be supported,

The encrypted file has following structure:

1. string `ArchiveKeep Encrypted File` followed by byte `0`,
2. file format - 8bit integer - currently constant `2`,
3. metadata section length - 32bit integer, 
4. metadata section contents:
   1. encrypt + combine + sign data: `JWE(plain + JWT(private))` 
   2. null-terminator (or else meaningful constant indicating follow-up contents),
   3. random data to fill the unused space (optional),
5. encrypted file data. 

All integers are stored in big endian system.

**Note:** random access support is aspect of cipher used for encrypted file data, currently it's AES-128-CFB. 
